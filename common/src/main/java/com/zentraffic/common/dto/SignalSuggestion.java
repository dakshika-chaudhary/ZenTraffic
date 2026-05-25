package com.zentraffic.common.dto;

public record SignalSuggestion(
        Long roadId,
        String roadName,
        int congestionScore,
        int recommendedGreenSeconds,
        String reason
) {
}
