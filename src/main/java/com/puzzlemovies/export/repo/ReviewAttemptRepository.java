package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ReviewAttempt;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewAttemptRepository extends JpaRepository<ReviewAttempt, UUID> {
    List<ReviewAttempt> findByReviewCardAndUserOrderByReviewedAtAsc(ReviewCard reviewCard, User user);

    List<ReviewAttempt> findByUserOrderByReviewedAtAsc(User user);
}
