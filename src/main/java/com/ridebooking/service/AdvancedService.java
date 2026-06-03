package com.ridebooking.service;

import com.ridebooking.domain.Payment;
import com.ridebooking.domain.PaymentMethod;
import com.ridebooking.domain.PaymentStatus;
import com.ridebooking.domain.Rating;
import com.ridebooking.domain.Ride;
import com.ridebooking.domain.RideStatus;
import com.ridebooking.domain.Role;
import com.ridebooking.domain.User;
import com.ridebooking.dto.AdvancedDtos;
import com.ridebooking.exception.ApiException;
import com.ridebooking.repository.DriverProfileRepository;
import com.ridebooking.repository.PaymentRepository;
import com.ridebooking.repository.RatingRepository;
import com.ridebooking.repository.RideRepository;
import com.ridebooking.repository.UserRepository;
import com.ridebooking.security.UserPrincipal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvancedService {
    private final RideService rideService;
    private final PaymentRepository payments;
    private final RatingRepository ratings;
    private final UserRepository users;
    private final DriverProfileRepository drivers;
    private final RideRepository rides;

    public AdvancedService(RideService rideService, PaymentRepository payments, RatingRepository ratings,
                           UserRepository users, DriverProfileRepository drivers, RideRepository rides) {
        this.rideService = rideService;
        this.payments = payments;
        this.ratings = ratings;
        this.users = users;
        this.drivers = drivers;
        this.rides = rides;
    }

    @Transactional
    public AdvancedDtos.PaymentResponse pay(UserPrincipal principal, java.util.UUID rideId, AdvancedDtos.PaymentRequest request) {
        Ride ride = rideService.ride(rideId);
        User user = rideService.currentUser(principal);
        if (!ride.getRider().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the rider can pay for this ride");
        }
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment is allowed only after ride completion");
        }

        Payment payment = payments.findByRide(ride).orElseGet(Payment::new);
        payment.setRide(ride);
        payment.setAmount(ride.getFare());
        payment.setMethod(request.method());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderReference(request.method() == PaymentMethod.CASH ? "cash-collected" : "demo-" + ride.getId());
        return toResponse(payments.save(payment));
    }

    @Transactional
    public AdvancedDtos.RatingResponse rate(UserPrincipal principal, java.util.UUID rideId, AdvancedDtos.RatingRequest request) {
        Ride ride = rideService.ride(rideId);
        User reviewer = rideService.currentUser(principal);
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "Rating is allowed only after ride completion");
        }
        if (ride.getDriver() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Ride has no driver to rate");
        }

        User reviewee;
        if (ride.getRider().getId().equals(reviewer.getId())) {
            reviewee = ride.getDriver().getUser();
        } else if (ride.getDriver().getUser().getId().equals(reviewer.getId())) {
            reviewee = ride.getRider();
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot rate this ride");
        }

        Rating rating = new Rating();
        rating.setRide(ride);
        rating.setReviewer(reviewer);
        rating.setReviewee(reviewee);
        rating.setScore(request.score());
        rating.setComment(request.comment());
        Rating saved = ratings.save(rating);
        updateDriverRating(reviewee);
        return new AdvancedDtos.RatingResponse(saved.getId(), ride.getId(), saved.getScore(), saved.getComment());
    }

    @Transactional(readOnly = true)
    public AdvancedDtos.AdminStatsResponse stats() {
        return new AdvancedDtos.AdminStatsResponse(
                users.countByRole(Role.ROLE_RIDER),
                users.countByRole(Role.ROLE_DRIVER),
                rides.countByStatusIn(List.of(RideStatus.REQUESTED, RideStatus.DRIVER_ASSIGNED, RideStatus.ACCEPTED, RideStatus.ARRIVED, RideStatus.IN_PROGRESS)),
                rides.countByStatus(RideStatus.COMPLETED)
        );
    }

    private void updateDriverRating(User reviewee) {
        drivers.findByUser(reviewee).ifPresent(driver -> {
            List<Rating> driverRatings = ratings.findByReviewee(reviewee);
            double average = driverRatings.stream().mapToInt(Rating::getScore).average().orElse(5.0);
            driver.setRating(average);
        });
    }

    private AdvancedDtos.PaymentResponse toResponse(Payment payment) {
        return new AdvancedDtos.PaymentResponse(
                payment.getId(),
                payment.getRide().getId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getProviderReference()
        );
    }
}
