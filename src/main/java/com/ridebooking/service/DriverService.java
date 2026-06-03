package com.ridebooking.service;

import com.ridebooking.domain.DriverProfile;
import com.ridebooking.domain.RideStatus;
import com.ridebooking.dto.DriverDtos;
import com.ridebooking.repository.PaymentRepository;
import com.ridebooking.repository.RideRepository;
import com.ridebooking.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {
    private final RideService rideService;
    private final PaymentRepository payments;
    private final RideRepository rides;

    public DriverService(RideService rideService, PaymentRepository payments, RideRepository rides) {
        this.rideService = rideService;
        this.payments = payments;
        this.rides = rides;
    }

    @Transactional
    public DriverDtos.DriverProfileResponse updateLocation(UserPrincipal principal, DriverDtos.LocationUpdateRequest request) {
        DriverProfile driver = rideService.currentDriver(principal);
        driver.setCurrentLatitude(request.latitude());
        driver.setCurrentLongitude(request.longitude());
        driver.setAvailable(request.available());
        return toResponse(driver);
    }

    @Transactional(readOnly = true)
    public DriverDtos.DriverProfileResponse me(UserPrincipal principal) {
        return toResponse(rideService.currentDriver(principal));
    }

    @Transactional(readOnly = true)
    public DriverDtos.EarningsResponse earnings(UserPrincipal principal) {
        DriverProfile driver = rideService.currentDriver(principal);
        return new DriverDtos.EarningsResponse(
                payments.sumSuccessfulPaymentsByDriver(driver),
                rides.findByDriverOrderByRequestedAtDesc(driver).stream()
                        .filter(ride -> ride.getStatus() == RideStatus.COMPLETED)
                        .count()
        );
    }

    private DriverDtos.DriverProfileResponse toResponse(DriverProfile driver) {
        return new DriverDtos.DriverProfileResponse(
                driver.getId(),
                driver.getUser().getId(),
                driver.getUser().getName(),
                driver.getVehicleNumber(),
                driver.getVehicleModel(),
                driver.isAvailable(),
                driver.getCurrentLatitude(),
                driver.getCurrentLongitude(),
                driver.getRating()
        );
    }
}
