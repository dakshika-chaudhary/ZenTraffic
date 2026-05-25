package com.zentraffic.common.dto;

import java.time.Instant;

public record UserProfile(Long id, String name, String email, String role, Instant createdAt) {
}
