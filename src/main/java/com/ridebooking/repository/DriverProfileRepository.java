package com.ridebooking.repository;

import com.ridebooking.domain.DriverProfile;
import com.ridebooking.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    Optional<DriverProfile> findByUser(User user);
    boolean existsByLicenseNumber(String licenseNumber);
    List<DriverProfile> findByAvailableTrueAndCurrentLatitudeIsNotNullAndCurrentLongitudeIsNotNull();
}
