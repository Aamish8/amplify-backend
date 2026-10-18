package com.amplify.amplify_backend.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
@ManyToOne private AppUser user;
private Long campaignId;
private String type;      // MILESTONE, STATUS, UPDATE
private String message;
@Builder.Default private boolean isRead = false;
private Instant createdAt;
@PrePersist void onCreate() { createdAt = Instant.now(); 
}
}
