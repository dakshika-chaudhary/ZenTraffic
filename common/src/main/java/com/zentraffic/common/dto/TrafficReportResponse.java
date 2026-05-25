package com.zentraffic.common.dto;

import java.time.Instant;

public record TrafficReportResponse(
        Long id,
        Long roadId,
        String roadName,
        String reportType,
        int severity,
        String status,
        Instant createdAt
) {
}
