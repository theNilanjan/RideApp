package com.ridebooking.controller;

import com.ridebooking.dto.DriverDtos;
import com.ridebooking.dto.RideDtos;
import com.ridebooking.security.UserPrincipal;
import com.ridebooking.service.DriverService;
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
@RequestMapping("/api/v1/drivers")
@PreAuthorize("hasRole('DRIVER')")
public class DriverController {
    private final DriverService drivers;
    private final RideService rides;

    public DriverController(DriverService drivers, RideService rides) {
        this.drivers = drivers;
        this.rides = rides;
    }

    @GetMapping("/me")
    DriverDtos.DriverProfileResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return drivers.me(principal);
    }

    @PatchMapping("/me/location")
    DriverDtos.DriverProfileResponse updateLocation(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody DriverDtos.LocationUpdateRequest request) {
        return drivers.updateLocation(principal, request);
    }

    @GetMapping("/me/rides")
    List<RideDtos.RideResponse> rideHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return rides.driverHistory(principal);
    }

    @PostMapping("/rides/{rideId}/decision")
    RideDtos.RideResponse decide(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID rideId,
                                 @Valid @RequestBody RideDtos.DriverDecisionRequest request) {
        return rides.decide(principal, rideId, request);
    }

    @PatchMapping("/rides/{rideId}/status")
    RideDtos.RideResponse updateRideStatus(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID rideId,
                                           @Valid @RequestBody RideDtos.UpdateRideStatusRequest request) {
        return rides.updateStatus(principal, rideId, request);
    }

    @GetMapping("/me/earnings")
    DriverDtos.EarningsResponse earnings(@AuthenticationPrincipal UserPrincipal principal) {
        return drivers.earnings(principal);
    }
}
