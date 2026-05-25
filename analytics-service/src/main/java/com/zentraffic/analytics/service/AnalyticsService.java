package com.zentraffic.analytics.service;

import com.zentraffic.common.dto.AnalyticsSummary;
import com.zentraffic.common.dto.RoadDto;
import com.zentraffic.common.dto.SignalSuggestion;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final RestClient trafficClient;

    public AnalyticsService(RestClient.Builder restClientBuilder) {
        this.trafficClient = restClientBuilder.baseUrl("http://TRAFFIC-SERVICE").build();
    }

    public AnalyticsSummary summary() {
        List<RoadDto> roads = liveRoads();
        double averageSpeed = roads.stream().mapToDouble(RoadDto::avgSpeed).average().orElse(0);
        List<RoadDto> hotspots = roads.stream()
                .sorted(Comparator.comparingInt(RoadDto::congestionScore).reversed())
                .limit(5)
                .toList();
        Map<String, Long> status = roads.stream().collect(Collectors.groupingBy(RoadDto::status, Collectors.counting()));
        long congested = roads.stream().filter(road -> road.congestionScore() >= 45).count();
        return new AnalyticsSummary(roads.size(), congested, Math.round(averageSpeed * 10.0) / 10.0, hotspots, status);
    }

    public Map<String, Object> peakHourAnalysis() {
        int hour = LocalTime.now().getHour();
        boolean peak = (hour >= 8 && hour <= 11) || (hour >= 17 && hour <= 21);
        List<RoadDto> roads = liveRoads();
        return Map.of(
                "currentHour", hour,
                "peakWindow", peak,
                "rule", "Peak hours are 08:00-11:00 and 17:00-21:00",
                "expectedCongestionMultiplier", peak ? 1.35 : 0.85,
                "busiestRoads", roads.stream().sorted(Comparator.comparingInt(RoadDto::congestionScore).reversed()).limit(3).toList()
        );
    }

    public List<Map<String, Object>> congestionTrends() {
        return liveRoads().stream()
                .map(road -> {
                    Map<String, Object> trend = new LinkedHashMap<>();
                    trend.put("roadId", road.id());
                    trend.put("roadName", road.roadName());
                    trend.put("currentScore", road.congestionScore());
                    trend.put("trend", road.congestionScore() >= 75 ? "SPIKING" : road.congestionScore() >= 45 ? "RISING" : "STABLE");
                    return trend;
                })
                .toList();
    }

    public List<SignalSuggestion> signalSuggestions() {
        SignalSuggestion[] suggestions = trafficClient.get().uri("/traffic/signals/suggestions").retrieve().body(SignalSuggestion[].class);
        return suggestions == null ? List.of() : Arrays.asList(suggestions);
    }

    public List<RoadDto> heatmap() {
        RoadDto[] roads = trafficClient.get().uri("/traffic/heatmap").retrieve().body(RoadDto[].class);
        return roads == null ? List.of() : Arrays.asList(roads);
    }

    private List<RoadDto> liveRoads() {
        RoadDto[] roads = trafficClient.get().uri("/traffic/live").retrieve().body(RoadDto[].class);
        return roads == null ? List.of() : Arrays.asList(roads);
    }
}
