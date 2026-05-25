package com.zentraffic.common.event;

import java.time.Instant;

public record CongestionEvent(Long roadId, String roadName, int congestionScore, String status, Instant detectedAt) {
}
