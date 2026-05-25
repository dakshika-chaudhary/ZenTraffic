package com.zentraffic.common.event;

import java.time.Instant;

public record AccidentReportedEvent(
        Long reportId,
        Long roadId,
        String roadName,
        int severity,
        String description,
        Instant occurredAt
) {
}
