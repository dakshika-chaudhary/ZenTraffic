package com.zentraffic.common.dto;

public record RegistrationOtpResponse(String message, String email, Integer expiresInSeconds, String devOtp) {
}
