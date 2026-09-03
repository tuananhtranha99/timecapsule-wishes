package com.timecapsule.wishes.repository;

import com.timecapsule.wishes.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findAllByRecipientIdAndRecipientUserIdOrderByOccurredAtDesc(UUID recipientId, UUID userId);

    Optional<Milestone> findByIdAndRecipientUserId(UUID id, UUID userId);

    List<Milestone> findAllByIdInAndRecipientUserId(Collection<UUID> ids, UUID userId);
}
