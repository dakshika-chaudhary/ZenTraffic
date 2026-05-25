package com.zentraffic.common.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(Long userId, @NotBlank String message, String priority) {
}
