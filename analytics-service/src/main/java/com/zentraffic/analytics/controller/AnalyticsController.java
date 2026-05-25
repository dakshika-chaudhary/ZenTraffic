package com.zentraffic.analytics.controller;

import com.zentraffic.analytics.service.AnalyticsService;
import com.zentraffic.common.dto.AnalyticsSummary;
import com.zentraffic.common.dto.RoadDto;
import com.zentraffic.common.dto.SignalSuggestion;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummary summary() {
        return analyticsService.summary();
    }

    @GetMapping("/peak-hours")
    public Map<String, Object> peakHours() {
        return analyticsService.peakHourAnalysis();
    }

    @GetMapping("/trends")
    public List<Map<String, Object>> trends() {
        return analyticsService.congestionTrends();
    }

    @GetMapping("/signals")
    public List<SignalSuggestion> signals() {
        return analyticsService.signalSuggestions();
    }

    @GetMapping("/heatmap")
    public List<RoadDto> heatmap() {
        return analyticsService.heatmap();
    }
}
