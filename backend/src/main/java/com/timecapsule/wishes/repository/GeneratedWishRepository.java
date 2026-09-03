package com.timecapsule.wishes.repository;

import com.timecapsule.wishes.entity.GeneratedWish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedWishRepository extends JpaRepository<GeneratedWish, UUID> {

    List<GeneratedWish> findAllByRecipientIdAndRecipientUserIdOrderByCreatedAtDesc(UUID recipientId, UUID userId);

    Optional<GeneratedWish> findByIdAndRecipientUserId(UUID id, UUID userId);
}
