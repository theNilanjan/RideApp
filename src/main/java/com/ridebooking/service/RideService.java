package com.ridebooking.service;

import com.ridebooking.domain.DriverProfile;
import com.ridebooking.domain.Ride;
import com.ridebooking.domain.RideStatus;
import com.ridebooking.domain.Role;
import com.ridebooking.domain.User;
import com.ridebooking.dto.RideDtos;
import com.ridebooking.exception.ApiException;
import com.ridebooking.repository.DriverProfileRepository;
import com.ridebooking.repository.RideRepository;
import com.ridebooking.repository.UserRepository;
import com.ridebooking.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final DriverProfileRepository drivers;
    private final RideRepository rides;
    private final SimpMessagingTemplate messaging;
    private final BigDecimal baseFare;
    private final BigDecimal perKm;
    private final BigDecimal perMinute;

    public RideService(UserRepository users, DriverProfileRepository drivers, RideRepository rides,
                       SimpMessagingTemplate messaging,
                       @Value("${app.pricing.base-fare}") BigDecimal baseFare,
                       @Value("${app.pricing.per-km}") BigDecimal perKm,
                       @Value("${app.pricing.per-minute}") BigDecimal perMinute) {
        this.users = users;
        this.drivers = drivers;
        this.rides = rides;
        this.messaging = messaging;
        this.baseFare = baseFare;
        this.perKm = perKm;
        this.perMinute = perMinute;
    }

    public RideDtos.FareEstimateResponse estimate(RideDtos.FareEstimateRequest request) {
        double distance = distanceKm(request.pickup(), request.dropoff());
        double surge = surgeMultiplier();
        return new RideDtos.FareEstimateResponse(calculateFare(distance, surge), round(distance), surge);
    }

    @Transactional
    public RideDtos.RideResponse book(UserPrincipal principal, RideDtos.BookRideRequest request) {
        User rider = currentUser(principal);
        if (rider.getRole() != Role.ROLE_RIDER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only riders can book rides");
        }

        double distance = distanceKm(request.pickup(), request.dropoff());
        double surge = surgeMultiplier();
        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setPickupLatitude(request.pickup().latitude());
        ride.setPickupLongitude(request.pickup().longitude());
        ride.setDropoffLatitude(request.dropoff().latitude());
        ride.setDropoffLongitude(request.dropoff().longitude());
        ride.setFare(calculateFare(distance, surge));
        ride.setSurgeMultiplier(surge);
        ride.setOtp(String.valueOf(1000 + RANDOM.nextInt(9000)));

        nearestAvailableDriver(request.pickup()).ifPresent(driver -> {
            ride.setDriver(driver);
            ride.setStatus(RideStatus.DRIVER_ASSIGNED);
            driver.setAvailable(false);
        });

        Ride saved = rides.save(ride);
        publish(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideResponse> riderHistory(UserPrincipal principal) {
        return rides.findByRiderOrderByRequestedAtDesc(currentUser(principal)).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideResponse> driverHistory(UserPrincipal principal) {
        DriverProfile driver = currentDriver(principal);
        return rides.findByDriverOrderByRequestedAtDesc(driver).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RideDtos.RideResponse cancel(UserPrincipal principal, UUID rideId, RideDtos.CancelRideRequest request) {
        Ride ride = ride(rideId);
        User user = currentUser(principal);
        boolean ownsRide = ride.getRider().getId().equals(user.getId());
        boolean assignedDriver = ride.getDriver() != null && ride.getDriver().getUser().getId().equals(user.getId());
        if (!ownsRide && !assignedDriver) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot cancel this ride");
        }
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Ride is already closed");
        }
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason(request.reason());
        if (ride.getDriver() != null) {
            ride.getDriver().setAvailable(true);
        }
        publish(ride);
        return toResponse(ride);
    }

    @Transactional
    public RideDtos.RideResponse decide(UserPrincipal principal, UUID rideId, RideDtos.DriverDecisionRequest request) {
        DriverProfile driver = currentDriver(principal);
        Ride ride = ride(rideId);
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driver.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ride is not assigned to this driver");
        }
        if (ride.getStatus() != RideStatus.DRIVER_ASSIGNED) {
            throw new ApiException(HttpStatus.CONFLICT, "Ride is not waiting for driver decision");
        }

        if (request.accept()) {
            ride.setStatus(RideStatus.ACCEPTED);
            ride.setAcceptedAt(Instant.now());
            driver.setAvailable(false);
        } else {
            ride.setStatus(RideStatus.REJECTED);
            ride.setDriver(null);
            driver.setAvailable(true);
        }
        publish(ride);
        return toResponse(ride);
    }

    @Transactional
    public RideDtos.RideResponse updateStatus(UserPrincipal principal, UUID rideId, RideDtos.UpdateRideStatusRequest request) {
        DriverProfile driver = currentDriver(principal);
        Ride ride = ride(rideId);
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driver.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ride is not assigned to this driver");
        }

        RideStatus next = request.status();
        if (next == RideStatus.ARRIVED && ride.getStatus() != RideStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Driver can arrive only after accepting");
        }
        if (next == RideStatus.IN_PROGRESS) {
            if (ride.getStatus() != RideStatus.ARRIVED) {
                throw new ApiException(HttpStatus.CONFLICT, "Ride can start only after driver arrival");
            }
            if (!ride.getOtp().equals(request.otp())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid ride OTP");
            }
            ride.setStartedAt(Instant.now());
        }
        if (next == RideStatus.COMPLETED) {
            if (ride.getStatus() != RideStatus.IN_PROGRESS) {
                throw new ApiException(HttpStatus.CONFLICT, "Ride can complete only after it starts");
            }
            ride.setCompletedAt(Instant.now());
            driver.setAvailable(true);
        }
        if (next == RideStatus.ACCEPTED || next == RideStatus.DRIVER_ASSIGNED || next == RideStatus.REQUESTED || next == RideStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported status transition");
        }

        ride.setStatus(next);
        publish(ride);
        return toResponse(ride);
    }

    public Ride ride(UUID rideId) {
        return rides.findById(rideId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ride not found"));
    }

    public User currentUser(UserPrincipal principal) {
        return users.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public DriverProfile currentDriver(UserPrincipal principal) {
        return drivers.findByUser(currentUser(principal))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Driver profile not found"));
    }

    public RideDtos.RideResponse toResponse(Ride ride) {
        UUID driverId = ride.getDriver() == null ? null : ride.getDriver().getId();
        return new RideDtos.RideResponse(
                ride.getId(),
                ride.getRider().getId(),
                driverId,
                new RideDtos.Coordinate(ride.getPickupLatitude(), ride.getPickupLongitude()),
                new RideDtos.Coordinate(ride.getDropoffLatitude(), ride.getDropoffLongitude()),
                ride.getStatus(),
                ride.getFare(),
                ride.getSurgeMultiplier(),
                ride.getOtp(),
                ride.getRequestedAt(),
                ride.getUpdatedAt()
        );
    }

    private java.util.Optional<DriverProfile> nearestAvailableDriver(RideDtos.Coordinate pickup) {
        return drivers.findByAvailableTrueAndCurrentLatitudeIsNotNullAndCurrentLongitudeIsNotNull().stream()
                .min(Comparator.comparingDouble(driver -> distanceKm(
                        pickup.latitude(), pickup.longitude(), driver.getCurrentLatitude(), driver.getCurrentLongitude())));
    }

    private BigDecimal calculateFare(double distanceKm, double surge) {
        double minutes = Math.max(5, distanceKm / 30.0 * 60.0);
        return baseFare
                .add(perKm.multiply(BigDecimal.valueOf(distanceKm)))
                .add(perMinute.multiply(BigDecimal.valueOf(minutes)))
                .multiply(BigDecimal.valueOf(surge))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private double surgeMultiplier() {
        long active = rides.countByStatusIn(List.of(RideStatus.REQUESTED, RideStatus.DRIVER_ASSIGNED, RideStatus.ACCEPTED, RideStatus.ARRIVED, RideStatus.IN_PROGRESS));
        long available = drivers.findByAvailableTrueAndCurrentLatitudeIsNotNullAndCurrentLongitudeIsNotNull().size();
        return active > available * 2L ? 1.5 : 1.0;
    }

    private double distanceKm(RideDtos.Coordinate a, RideDtos.Coordinate b) {
        return distanceKm(a.latitude(), a.longitude(), b.latitude(), b.longitude());
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void publish(Ride ride) {
        messaging.convertAndSend("/topic/rides/" + ride.getId(), toResponse(ride));
    }
}
