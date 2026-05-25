package com.zentraffic.common.dto;

public record RoadDto(
        Long id,
        String roadName,
        String city,
        int currentDensity,
        double avgSpeed,
        String status,
        int congestionScore
) {
}
