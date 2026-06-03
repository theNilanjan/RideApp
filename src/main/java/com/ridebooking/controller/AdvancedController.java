package com.ridebooking.controller;

import com.ridebooking.dto.AdvancedDtos;
import com.ridebooking.security.UserPrincipal;
import com.ridebooking.service.AdvancedService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AdvancedController {
    private final AdvancedService advanced;

    public AdvancedController(AdvancedService advanced) {
        this.advanced = advanced;
    }

    @PostMapping("/rides/{rideId}/payments")
    @PreAuthorize("hasRole('RIDER')")
    AdvancedDtos.PaymentResponse pay(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID rideId,
                                     @Valid @RequestBody AdvancedDtos.PaymentRequest request) {
        return advanced.pay(principal, rideId, request);
    }

    @PostMapping("/rides/{rideId}/ratings")
    AdvancedDtos.RatingResponse rate(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID rideId,
                                     @Valid @RequestBody AdvancedDtos.RatingRequest request) {
        return advanced.rate(principal, rideId, request);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    AdvancedDtos.AdminStatsResponse stats() {
        return advanced.stats();
    }
}
