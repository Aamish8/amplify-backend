package com.amplify.amplify_backend.model;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.*;
//model/Contribution.java
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contribution {
@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
@ManyToOne private Campaign campaign;
@ManyToOne private AppUser user;
private Double amount;
private Double fee;
private Double total;
@Enumerated(EnumType.STRING) private PaymentStatus status;
private String receiptNo;
private Instant createdAt;
@PrePersist void onCreate() { createdAt = Instant.now(); }
}