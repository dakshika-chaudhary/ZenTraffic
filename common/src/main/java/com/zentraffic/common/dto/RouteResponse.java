package com.zentraffic.common.dto;

import java.util.List;

public record RouteResponse(
        List<String> path,
        double distanceKm,
        int estimatedMinutes,
        int congestionScore,
        int timeSavedMinutes,
        String strategy
) {
}
