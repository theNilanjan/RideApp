package com.ridebooking.dto;

import com.ridebooking.domain.RideStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class RideDtos {
    private RideDtos() {}

    public record Coordinate(
            @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") double longitude
    ) {}

    public record FareEstimateRequest(@NotNull Coordinate pickup, @NotNull Coordinate dropoff) {}

    public record FareEstimateResponse(BigDecimal estimatedFare, double distanceKm, double surgeMultiplier) {}

    public record BookRideRequest(@NotNull Coordinate pickup, @NotNull Coordinate dropoff) {}

    public record CancelRideRequest(@NotBlank @Size(max = 255) String reason) {}

    public record DriverDecisionRequest(boolean accept) {}

    public record UpdateRideStatusRequest(@NotNull RideStatus status, String otp) {}

    public record RideResponse(
            UUID rideId,
            UUID riderId,
            UUID driverId,
            Coordinate pickup,
            Coordinate dropoff,
            RideStatus status,
            BigDecimal fare,
            double surgeMultiplier,
            String otp,
            Instant requestedAt,
            Instant updatedAt
    ) {}
}
