package com.amplify.amplify_backend.web;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.amplify.amplify_backend.dto.Dtos.Breakdown;
import com.amplify.amplify_backend.dto.Dtos.ContributionRequest;
import com.amplify.amplify_backend.dto.Dtos.DashboardStats;
import com.amplify.amplify_backend.dto.Dtos.DraftRequest;
import com.amplify.amplify_backend.dto.Dtos.ReachEstimate;
import com.amplify.amplify_backend.dto.Dtos.SubmitRequest;
import com.amplify.amplify_backend.dto.Dtos.TimelineStep;
import com.amplify.amplify_backend.model.AppUser;
import com.amplify.amplify_backend.model.Campaign;
import com.amplify.amplify_backend.model.CampaignStatus;
import com.amplify.amplify_backend.model.Contribution;
import com.amplify.amplify_backend.model.Notification;
import com.amplify.amplify_backend.repo.NotificationRepository;
import com.amplify.amplify_backend.repo.UserRepository;
import com.amplify.amplify_backend.service.CampaignService;
import com.amplify.amplify_backend.service.ContributionService;
import com.amplify.amplify_backend.service.Pricing;

import java.util.*;
import lombok.RequiredArgsConstructor;

//web/ApiController.java
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ApiController {
private final CampaignService cs;
private final ContributionService ks;
private final Pricing pricing;
private final NotificationRepository notes;
private final UserRepository users;

@GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok"); }
@GetMapping("/me") public AppUser me() { return users.findById(CampaignService.DEMO_USER).orElseThrow(); }

@GetMapping("/campaigns")
public List<Campaign> feed(@RequestParam(required = false) String q, @RequestParam(required = false) String category,
                          @RequestParam(defaultValue = "trending") String sort) { return cs.feed(q, category, sort); }

@GetMapping("/campaigns/mine")
public List<Campaign> mine(@RequestParam(required = false) List<CampaignStatus> status) { return cs.mine(status); }

@GetMapping("/campaigns/{id}") public Campaign one(@PathVariable Long id) { return cs.get(id); }
@GetMapping("/campaigns/{id}/timeline") public List<TimelineStep> timeline(@PathVariable Long id) { return cs.timeline(id); }

@PostMapping("/campaigns/draft") @ResponseStatus(HttpStatus.CREATED)
public Campaign create(@RequestBody DraftRequest r) { return cs.createDraft(r); }

@PutMapping("/campaigns/{id}/draft") public Campaign save(@PathVariable Long id, @RequestBody DraftRequest r) { return cs.saveDraft(id, r); }
@PostMapping("/campaigns/{id}/submit") public Campaign submit(@PathVariable Long id, @RequestBody SubmitRequest r) { return cs.submit(id, r); }
@DeleteMapping("/campaigns/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) 
public void delete(@PathVariable Long id) { cs.deleteDraft(id); }

@GetMapping("/dashboard") public DashboardStats dashboard() { return cs.dashboard(); }

@GetMapping("/reach/estimate")
public ReachEstimate reach(@RequestParam double budget, @RequestParam(required = false) String locations,
                          @RequestParam(required = false) String interests) {
 return pricing.reach(budget, pricing.count(locations), pricing.count(interests));
}

@PostMapping("/contributions/preview") public Breakdown preview(@RequestBody ContributionRequest r) { return ks.preview(r.campaignId(), r.amount()); }
@PostMapping("/contributions") public Contribution pay(@RequestBody ContributionRequest r) { return ks.pay(r); }
@GetMapping("/contributions/mine") public List<Contribution> myContributions() { return ks.mine(); }
@GetMapping("/contributions/{id}") public Contribution receipt(@PathVariable Long id) { return ks.get(id); }

@GetMapping("/notifications")
public List<Notification> notifications() { return notes.findByUserIdOrderByCreatedAtDesc(CampaignService.DEMO_USER); }

@PostMapping("/notifications/read-all")
public void readAll() {
 var list = notes.findByUserIdOrderByCreatedAtDesc(CampaignService.DEMO_USER);
 list.forEach(n -> n.setRead(true));
 notes.saveAll(list);
}
}
