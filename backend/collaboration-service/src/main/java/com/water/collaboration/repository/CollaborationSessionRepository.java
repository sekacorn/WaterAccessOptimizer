package com.water.collaboration.repository;

import com.water.collaboration.model.CollaborationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CollaborationSessionRepository extends JpaRepository<CollaborationSession, UUID> {
    List<CollaborationSession> findByOwnerId(UUID ownerId);
    List<CollaborationSession> findByIsActive(Boolean isActive);
}
