package com.zentraffic.traffic.controller;

import com.zentraffic.common.dto.RoadDto;
import com.zentraffic.common.dto.SignalSuggestion;
import com.zentraffic.common.dto.TrafficReportRequest;
import com.zentraffic.common.dto.TrafficReportResponse;
import com.zentraffic.traffic.service.TrafficService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/traffic")
public class TrafficController {
    private final TrafficService trafficService;

    public TrafficController(TrafficService trafficService) {
        this.trafficService = trafficService;
    }

    @PostMapping("/report")
    public TrafficReportResponse report(@Valid @RequestBody TrafficReportRequest request) {
        return trafficService.report(request);
    }

    @GetMapping("/live")
    public List<RoadDto> live() {
        return trafficService.live();
    }

    @GetMapping("/congestion")
    public List<RoadDto> congestion() {
        return trafficService.congestion();
    }

    @GetMapping("/heatmap")
    public List<RoadDto> heatmap() {
        return trafficService.heatmap();
    }

    @GetMapping("/signals/suggestions")
    public List<SignalSuggestion> signalSuggestions() {
        return trafficService.signalSuggestions();
    }
}
