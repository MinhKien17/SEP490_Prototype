package com.evidencepilot.repository;

import com.evidencepilot.model.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, UUID> {
    List<SystemNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<SystemNotification> findByUserIdAndActorId(UUID userId, UUID actorId);
}
