package com.timecapsule.wishes.repository;

import com.timecapsule.wishes.entity.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, UUID> {

    List<Recipient> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Recipient> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
