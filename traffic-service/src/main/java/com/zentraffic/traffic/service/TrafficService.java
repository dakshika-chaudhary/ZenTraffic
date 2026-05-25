package com.zentraffic.traffic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zentraffic.common.dto.RoadDto;
import com.zentraffic.common.dto.SignalSuggestion;
import com.zentraffic.common.dto.TrafficReportRequest;
import com.zentraffic.common.dto.TrafficReportResponse;
import com.zentraffic.common.event.AccidentReportedEvent;
import com.zentraffic.common.event.CongestionEvent;
import com.zentraffic.common.event.RoadStatusEvent;
import com.zentraffic.traffic.entity.CongestionLog;
import com.zentraffic.traffic.entity.Road;
import com.zentraffic.traffic.entity.TrafficReport;
import com.zentraffic.traffic.repository.CongestionLogRepository;
import com.zentraffic.traffic.repository.RoadRepository;
import com.zentraffic.traffic.repository.TrafficReportRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class TrafficService {
    private final RoadRepository roads;
    private final TrafficReportRepository reports;
    private final CongestionLogRepository congestionLogs;
    private final TrafficRules rules;
    private final KafkaTemplate<String, Object> kafka;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public TrafficService(RoadRepository roads, TrafficReportRepository reports, CongestionLogRepository congestionLogs,
                          TrafficRules rules, KafkaTemplate<String, Object> kafka, StringRedisTemplate redis,
                          ObjectMapper objectMapper) {
        this.roads = roads;
        this.reports = reports;
        this.congestionLogs = congestionLogs;
        this.rules = rules;
        this.kafka = kafka;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TrafficReportResponse report(TrafficReportRequest request) {
        Road road = roads.findById(request.roadId()).orElseThrow();
        TrafficReport report = new TrafficReport();
        report.setUserId(request.userId());
        report.setRoadId(request.roadId());
        report.setReportType(request.reportType().toUpperCase());
        report.setSeverity(request.severity());
        report.setDescription(request.description());
        reports.save(report);

        int vehicleCount = request.vehicleCount() == null ? road.getCurrentDensity() : request.vehicleCount();
        double speed = request.observedSpeed() == null ? road.getAvgSpeed() : request.observedSpeed();
        int score = rules.congestionScore(vehicleCount, speed, request.severity());
        road.setCurrentDensity(vehicleCount);
        road.setAvgSpeed(speed);
        road.setStatus(rules.statusFor(score, request.reportType()));

        CongestionLog log = new CongestionLog();
        log.setRoadId(road.getId());
        log.setDensityScore(score);
        congestionLogs.save(log);

        RoadDto dto = dto(road);
        cache(dto);
        kafka.send("traffic.road.status", String.valueOf(road.getId()),
                new RoadStatusEvent(road.getId(), road.getRoadName(), road.getStatus(), road.getCurrentDensity(), road.getAvgSpeed(), Instant.now()));
        if ("ACCIDENT".equalsIgnoreCase(request.reportType())) {
            kafka.send("traffic.accident.reported", String.valueOf(road.getId()),
                    new AccidentReportedEvent(report.getId(), road.getId(), road.getRoadName(), request.severity(), request.description(), report.getCreatedAt()));
        }
        if (score >= 75) {
            kafka.send("traffic.congestion.detected", String.valueOf(road.getId()),
                    new CongestionEvent(road.getId(), road.getRoadName(), score, road.getStatus(), Instant.now()));
        }
        return new TrafficReportResponse(report.getId(), road.getId(), road.getRoadName(), report.getReportType(),
                report.getSeverity(), road.getStatus(), report.getCreatedAt());
    }

    public List<RoadDto> live() {
        return roads.findAll().stream().map(this::dto).toList();
    }

    public List<RoadDto> congestion() {
        return roads.findByStatusIn(List.of("HEAVY", "BLOCKED", "ACCIDENT")).stream()
                .map(this::dto)
                .sorted(Comparator.comparingInt(RoadDto::congestionScore).reversed())
                .toList();
    }

    public List<RoadDto> heatmap() {
        return live().stream().sorted(Comparator.comparingInt(RoadDto::congestionScore).reversed()).toList();
    }

    public List<SignalSuggestion> signalSuggestions() {
        return heatmap().stream()
                .filter(road -> road.congestionScore() >= 45)
                .map(road -> new SignalSuggestion(road.id(), road.roadName(), road.congestionScore(),
                        Math.min(90, 30 + road.congestionScore() / 2),
                        "Rule: increase green time when congestion score crosses 45"))
                .toList();
    }

    private RoadDto dto(Road road) {
        int score = rules.congestionScore(road.getCurrentDensity(), road.getAvgSpeed(), 0);
        return new RoadDto(road.getId(), road.getRoadName(), road.getCity(), road.getCurrentDensity(),
                road.getAvgSpeed(), road.getStatus(), score);
    }

    private void cache(RoadDto dto) {
        try {
            redis.opsForValue().set("traffic:road:" + dto.id(), objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException ignored) {
        }
    }
}
