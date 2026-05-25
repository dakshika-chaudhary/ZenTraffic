package com.zentraffic.common.dto;

import jakarta.validation.constraints.NotBlank;

public record RouteRequest(
        @NotBlank String source,
        @NotBlank String destination,
        String strategy,
        boolean emergencyVehicle
) {
}
