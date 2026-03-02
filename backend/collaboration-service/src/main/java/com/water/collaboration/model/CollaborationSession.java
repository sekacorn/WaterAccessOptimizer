package com.water.collaboration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collaboration_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollaborationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "session_name")
    private String sessionName;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(columnDefinition = "TEXT")
    private String participants; // JSON array of user IDs

    @Column(name = "session_type")
    private String sessionType; // plan, analysis, visualization

    @Column(columnDefinition = "TEXT")
    private String sharedData; // JSON data being collaborated on

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
