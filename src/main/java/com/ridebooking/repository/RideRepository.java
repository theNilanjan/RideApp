package com.ridebooking.repository;

import com.ridebooking.domain.DriverProfile;
import com.ridebooking.domain.Ride;
import com.ridebooking.domain.RideStatus;
import com.ridebooking.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    List<Ride> findByRiderOrderByRequestedAtDesc(User rider);
    List<Ride> findByDriverOrderByRequestedAtDesc(DriverProfile driver);
    long countByStatus(RideStatus status);
    long countByStatusIn(Collection<RideStatus> statuses);
}
