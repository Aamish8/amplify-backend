package com.amplify.amplify_backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.amplify.amplify_backend.dto.Dtos.DashboardStats;
import com.amplify.amplify_backend.dto.Dtos.DraftRequest;
import com.amplify.amplify_backend.dto.Dtos.SubmitRequest;
import com.amplify.amplify_backend.dto.Dtos.TimelineStep;
import com.amplify.amplify_backend.model.Campaign;
import com.amplify.amplify_backend.model.CampaignStatus;
import com.amplify.amplify_backend.model.Notification;
import com.amplify.amplify_backend.repo.CampaignRepository;
import com.amplify.amplify_backend.repo.NotificationRepository;
import com.amplify.amplify_backend.repo.UserRepository;
import com.amplify.amplify_backend.web.ApiException;

import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class CampaignService {
public static final long DEMO_USER = 1L;
private static final List<CampaignStatus> FLOW = List.of(CampaignStatus.DRAFT, CampaignStatus.SUBMITTED,
   CampaignStatus.APPROVED, CampaignStatus.FUNDING, CampaignStatus.GOAL_REACHED,
   CampaignStatus.DISTRIBUTION_PREP, CampaignStatus.PROMOTING, CampaignStatus.COMPLETED);

private final CampaignRepository repo;
private final UserRepository users;
private final NotificationRepository notes;
private final ModerationService moderation;
private final Pricing pricing;

public Campaign get(Long id) {
 return repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
}

public List<Campaign> feed(String q, String category, String sort) {
 var list = repo.findByStatusIn(List.of(CampaignStatus.FUNDING, CampaignStatus.GOAL_REACHED,
     CampaignStatus.PROMOTING, CampaignStatus.COMPLETED)).stream()
     .filter(c -> q == null || q.isBlank() || (c.getTitle() + " " + c.getSummary() + " " + c.getCreatorName()).toLowerCase().contains(q.toLowerCase()))
     .filter(c -> category == null || category.isBlank() || category.equalsIgnoreCase("All") || category.equalsIgnoreCase(c.getCategory()))
     .collect(Collectors.toList());
 Comparator<Campaign> cmp = switch (sort == null ? "trending" : sort) {
   case "newest" -> Comparator.comparing(Campaign::getCreatedAt).reversed();
   case "ending" -> Comparator.comparing(Campaign::getFundingEndsAt, Comparator.nullsLast(Comparator.naturalOrder()));
   case "funded" -> Comparator.comparingDouble((Campaign c) -> c.getRaised() / Math.max(c.getFundingGoal(), 1)).reversed();
   default -> Comparator.comparingInt(Campaign::getBackersCount).reversed();
 };
 list.sort(cmp);
 return list;
}

public List<Campaign> mine(List<CampaignStatus> statuses) {
 return repo.findByOwnerIdOrderByUpdatedAtDesc(DEMO_USER).stream()
     .filter(c -> statuses == null || statuses.isEmpty() || statuses.contains(c.getStatus())).toList();
}

public Campaign createDraft(DraftRequest r) {
 Campaign c = Campaign.builder().owner(users.findById(DEMO_USER).orElseThrow()).build();
 apply(c, r);
 return repo.save(c);
}

public Campaign saveDraft(Long id, DraftRequest r) {
 Campaign c = get(id);
 if (c.getStatus() != CampaignStatus.DRAFT && c.getStatus() != CampaignStatus.CHANGES_REQUESTED)
   throw new ApiException(HttpStatus.CONFLICT, "Only drafts can be edited");
 apply(c, r);
 return repo.save(c);
}

public void deleteDraft(Long id) {
 Campaign c = get(id);
 if (c.getStatus() != CampaignStatus.DRAFT) throw new ApiException(HttpStatus.CONFLICT, "Only drafts can be deleted");
 repo.delete(c);
}

private void apply(Campaign c, DraftRequest r) {
 if (r.title() != null) {
   if (r.title().length() > 60) throw new ApiException(HttpStatus.BAD_REQUEST, "Title must be 60 characters or fewer");
   c.setTitle(r.title());
 }
 if (r.category() != null) c.setCategory(r.category());
 if (r.creatorName() != null) c.setCreatorName(r.creatorName());
 if (r.summary() != null) c.setSummary(r.summary());
 if (r.coverImageUrl() != null) c.setCoverImageUrl(r.coverImageUrl());
 if (r.storyHeadline() != null) c.setStoryHeadline(r.storyHeadline());
 if (r.storyIntro() != null) c.setStoryIntro(r.storyIntro());
 if (r.storySections() != null) c.setStorySections(r.storySections());
 if (r.sources() != null) c.setSources(r.sources());
 if (r.adHeadline() != null) c.setAdHeadline(r.adHeadline());
 if (r.adDescription() != null) c.setAdDescription(r.adDescription());
 if (r.adCta() != null) c.setAdCta(r.adCta());
 if (r.adCreatives() != null) c.setAdCreatives(r.adCreatives());
 if (r.locations() != null) c.setLocations(r.locations());
 if (r.interests() != null) c.setInterests(r.interests());
 if (r.durationDays() != null) c.setDurationDays(r.durationDays());
 if (r.fundingGoal() != null) c.setFundingGoal(r.fundingGoal());
 if (r.currentStep() != null) c.setCurrentStep(Math.min(5, Math.max(c.getCurrentStep(), r.currentStep())));
 if (r.adBudget() != null) c.setAdBudget(r.adBudget());
 if (c.getAdBudget() != null)
   c.setEstimatedReach(pricing.reach(c.getAdBudget(), pricing.count(c.getLocations()), pricing.count(c.getInterests())).expected());
}

public Campaign submit(Long id, SubmitRequest r) {
 Campaign c = get(id);
 if (c.getStatus() != CampaignStatus.DRAFT && c.getStatus() != CampaignStatus.CHANGES_REQUESTED)
   throw new ApiException(HttpStatus.CONFLICT, "This campaign was already submitted");
 if (!Boolean.TRUE.equals(r.policyAccepted()))
   throw new ApiException(HttpStatus.BAD_REQUEST, "Please confirm the campaign rules before submitting");
 if (r.fundingGoal() == null || r.fundingGoal() <= 0)
   throw new ApiException(HttpStatus.BAD_REQUEST, "Set a funding goal greater than 0");

 c.setFundingGoal(r.fundingGoal());
 c.setStatus(CampaignStatus.SUBMITTED);
 repo.save(c);

 var res = moderation.review(c);
 c.setModerationFeedback(res.feedback());
 if (res.status() == CampaignStatus.APPROVED) {
   c.setStatus(CampaignStatus.FUNDING);
   c.setFundingEndsAt(Instant.now().plus(Duration.ofDays(c.getDurationDays() == null ? 30 : c.getDurationDays())));
 } else {
   c.setStatus(res.status());
 }
 repo.save(c);
 notify(c, "STATUS", switch (c.getStatus()) {
   case FUNDING -> "\"" + c.getTitle() + "\" was approved and is now open for funding.";
   case CHANGES_REQUESTED -> "Changes requested for \"" + c.getTitle() + "\". Open it to see feedback.";
   default -> "\"" + c.getTitle() + "\" was not approved.";
 });
 return c;
}

public List<TimelineStep> timeline(Long id) {
 Campaign c = get(id);
 CampaignStatus s = c.getStatus();
 List<TimelineStep> out = new ArrayList<>();
 if (!FLOW.contains(s)) {
   out.add(new TimelineStep("DRAFT", "Draft", "done"));
   out.add(new TimelineStep("SUBMITTED", "Submitted for review", "done"));
   out.add(new TimelineStep(s.name(), pretty(s), "current"));
   return out;
 }
 int idx = FLOW.indexOf(s);
 for (int i = 0; i < FLOW.size(); i++)
   out.add(new TimelineStep(FLOW.get(i).name(), pretty(FLOW.get(i)), i < idx ? "done" : i == idx ? "current" : "pending"));
 return out;
}

public DashboardStats dashboard() {
 var all = mine(null);
 Map<String, Long> counts = all.stream().collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));
 return new DashboardStats(counts,
     all.stream().mapToDouble(Campaign::getRaised).sum(),
     all.stream().mapToDouble(c -> c.getAdBudget() == null ? 0 : c.getAdBudget()).sum(),
     all.stream().mapToDouble(c -> c.getAdSpent() == null ? 0 : c.getAdSpent()).sum(),
     all.stream().mapToLong(c -> c.getEstimatedReach() == null ? 0 : c.getEstimatedReach()).sum(),
     all.stream().mapToLong(c -> c.getMeasuredReach() == null ? 0 : c.getMeasuredReach()).sum());
}

public void notify(Campaign c, String type, String msg) {
 notes.save(Notification.builder().user(c.getOwner()).campaignId(c.getId()).type(type).message(msg).build());
}

private String pretty(CampaignStatus s) {
 return switch (s) {
   case SUBMITTED -> "Submitted for review";
   case APPROVED -> "Approved";
   case FUNDING -> "Available for funding";
   case GOAL_REACHED -> "Funding goal reached";
   case DISTRIBUTION_PREP -> "Distribution preparation";
   default -> s.name().charAt(0) + s.name().substring(1).toLowerCase().replace('_', ' ');
 };
}
}
