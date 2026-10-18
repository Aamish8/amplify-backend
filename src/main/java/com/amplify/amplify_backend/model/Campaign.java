package com.amplify.amplify_backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Size;
import lombok.*;
//model/Campaign.java
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Campaign {
@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
@ManyToOne private AppUser owner;

// Step 1
@Size(max = 60) private String title;
private String category;
private String creatorName;
@Column(length = 500) private String summary;
private String coverImageUrl;

// Step 2 (JSON stored as text)
private String storyHeadline;
@Column(columnDefinition = "TEXT") private String storyIntro;
@Column(columnDefinition = "TEXT") private String storySections;
@Column(columnDefinition = "TEXT") private String sources;

// Step 3
private String adHeadline;
@Column(length = 500) private String adDescription;
private String adCta;
@Column(columnDefinition = "TEXT") private String adCreatives;

// Step 4
@Column(columnDefinition = "TEXT") private String locations;
@Column(columnDefinition = "TEXT") private String interests;
private Double adBudget;
private Integer durationDays;
private Long estimatedReach;

// Step 5 + lifecycle
private Double fundingGoal;
@Builder.Default private Double raised = 0.0;
@Builder.Default private Integer backersCount = 0;
private Double adSpent;
private Long measuredReach;
@Builder.Default private Integer currentStep = 1;
@Enumerated(EnumType.STRING) @Builder.Default private CampaignStatus status = CampaignStatus.DRAFT;
@Column(columnDefinition = "TEXT") private String moderationFeedback;
private Instant fundingEndsAt;
private Instant createdAt;
private Instant updatedAt;

@PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
@PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
