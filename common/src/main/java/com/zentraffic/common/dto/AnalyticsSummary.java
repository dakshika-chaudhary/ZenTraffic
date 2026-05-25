package com.zentraffic.common.dto;

import java.util.List;
import java.util.Map;

public record AnalyticsSummary(
        int monitoredRoads,
        long congestedRoads,
        double averageSpeed,
        List<RoadDto> hotspots,
        Map<String, Long> roadStatusBreakdown
) {
}
