package com.zentraffic.common.event;

import java.time.Instant;

public record RoadStatusEvent(Long roadId, String roadName, String status, int density, double avgSpeed, Instant updatedAt) {
}
