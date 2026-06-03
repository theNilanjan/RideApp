package com.ridebooking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;

public final class DriverDtos {
    private DriverDtos() {}

    public record LocationUpdateRequest(
            @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            boolean available
    ) {}

    public record DriverProfileResponse(
            UUID driverId,
            UUID userId,
            String name,
            String vehicleNumber,
            String vehicleModel,
            boolean available,
            Double latitude,
            Double longitude,
            double rating
    ) {}

    public record EarningsResponse(BigDecimal totalEarnings, long completedRides) {}
}
