package com.amplify.amplify_backend.dto;

import java.util.Map;

public class Dtos {
	  public record DraftRequest(String title, String category, String creatorName, String summary,
	      String coverImageUrl, String storyHeadline, String storyIntro, String storySections,
	      String sources, String adHeadline, String adDescription, String adCta, String adCreatives,
	      String locations, String interests, Double adBudget, Integer durationDays,
	      Double fundingGoal, Integer currentStep) {}
	  public record SubmitRequest(Double fundingGoal, Boolean policyAccepted) {}
	  public record ContributionRequest(Long campaignId, Double amount, String simulate) {}
	  public record Breakdown(double amount, double platformFee, double adAllocation,
	      double processingFee, double total, long estimatedReach) {}
	  public record ReachEstimate(long min, long max, long expected) {}
	  public record TimelineStep(String key, String label, String state) {}
	  public record DashboardStats(Map<String, Long> counts, double totalRaised, double totalAdBudget,
	      double totalAdSpent, long estimatedReach, long measuredReach) {}
	}
