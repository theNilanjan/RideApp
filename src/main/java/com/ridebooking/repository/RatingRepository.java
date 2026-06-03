package com.ridebooking.repository;

import com.ridebooking.domain.Rating;
import com.ridebooking.domain.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    List<Rating> findByReviewee(User reviewee);
}
