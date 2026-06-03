package com.ridebooking.dto;

import com.ridebooking.domain.PaymentMethod;
import com.ridebooking.domain.PaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class AdvancedDtos {
    private AdvancedDtos() {}

    public record PaymentRequest(@NotNull PaymentMethod method) {}

    public record PaymentResponse(UUID paymentId, UUID rideId, BigDecimal amount, PaymentMethod method, PaymentStatus status, String providerReference) {}

    public record RatingRequest(@Min(1) @Max(5) int score, @Size(max = 500) String comment) {}

    public record RatingResponse(UUID ratingId, UUID rideId, int score, String comment) {}

    public record AdminStatsResponse(long riders, long drivers, long activeRides, long completedRides) {}
}
