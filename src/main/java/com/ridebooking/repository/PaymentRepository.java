package com.ridebooking.repository;

import com.ridebooking.domain.Payment;
import com.ridebooking.domain.Ride;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByRide(Ride ride);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.ride.driver = :driver
              and p.status = com.ridebooking.domain.PaymentStatus.SUCCESS
            """)
    BigDecimal sumSuccessfulPaymentsByDriver(@Param("driver") com.ridebooking.domain.DriverProfile driver);
}
