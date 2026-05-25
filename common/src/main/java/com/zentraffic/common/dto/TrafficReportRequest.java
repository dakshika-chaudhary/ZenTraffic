package com.zentraffic.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrafficReportRequest(
        @NotNull Long userId,
        @NotNull Long roadId,
        @NotBlank String reportType,
        int severity,
        String description,
        Integer vehicleCount,
        Double observedSpeed
) {
}
