package com.ridebooking.controller;

import com.ridebooking.dto.RideDtos;
import com.ridebooking.security.UserPrincipal;
import com.ridebooking.service.RideService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {
    private final RideService rides;

    public RideController(RideService rides) {
        this.rides = rides;
    }

    @PostMapping("/estimate")
    RideDtos.FareEstimateResponse estimate(@Valid @RequestBody RideDtos.FareEstimateRequest request) {
        return rides.estimate(request);
    }

    @PostMapping
    @PreAuthorize("hasRole('RIDER')")
    RideDtos.RideResponse book(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody RideDtos.BookRideRequest request) {
        return rides.book(principal, request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RIDER')")
    List<RideDtos.RideResponse> riderHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return rides.riderHistory(principal);
    }

    @PatchMapping("/{rideId}/cancel")
    RideDtos.RideResponse cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID rideId,
                                 @Valid @RequestBody RideDtos.CancelRideRequest request) {
        return rides.cancel(principal, rideId, request);
    }
}
