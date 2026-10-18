package com.amplify.amplify_backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.amplify.amplify_backend.dto.Dtos.Breakdown;
import com.amplify.amplify_backend.dto.Dtos.ContributionRequest;
import com.amplify.amplify_backend.model.Campaign;
import com.amplify.amplify_backend.model.CampaignStatus;
import com.amplify.amplify_backend.model.Contribution;
import com.amplify.amplify_backend.model.PaymentStatus;
import com.amplify.amplify_backend.repo.CampaignRepository;
import com.amplify.amplify_backend.repo.ContributionRepository;
import com.amplify.amplify_backend.repo.UserRepository;
import com.amplify.amplify_backend.web.ApiException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.*;
//service/ContributionService.java
@Service @RequiredArgsConstructor
public class ContributionService {
private final ContributionRepository contributions;
private final CampaignRepository campaigns;
private final UserRepository users;
private final CampaignService campaignService;
private final Pricing pricing;

public Breakdown preview(Long campaignId, Double amount) {
 return pricing.breakdown(fundable(campaignId), validAmount(amount));
}

@Transactional
public Contribution pay(ContributionRequest r) {
 Campaign c = fundable(r.campaignId());
 double amount = validAmount(r.amount());
 Breakdown b = pricing.breakdown(c, amount);
 String sim = r.simulate() == null ? "success" : r.simulate().toLowerCase();
 PaymentStatus st = switch (sim) { case "fail" -> PaymentStatus.FAILED; case "pending" -> PaymentStatus.PENDING; default -> PaymentStatus.SUCCESS; };
 Contribution k = contributions.save(Contribution.builder().campaign(c)
     .user(users.findById(CampaignService.DEMO_USER).orElseThrow())
     .amount(amount).fee(pricing.r2(b.platformFee() + b.processingFee())).total(b.total())
     .status(st).receiptNo("AMP-" + (100000 + new Random().nextInt(900000))).build());
 if (st == PaymentStatus.SUCCESS) settle(k);
 return k;
}

/** Pending payments auto-succeed after 4s so the app can poll this. */
@Transactional
public Contribution get(Long id) {
 Contribution k = contributions.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Receipt not found"));
 if (k.getStatus() == PaymentStatus.PENDING && k.getCreatedAt().plusSeconds(4).isBefore(Instant.now())) settle(k);
 return k;
}

public List<Contribution> mine() { return contributions.findByUserIdOrderByCreatedAtDesc(CampaignService.DEMO_USER); }

private void settle(Contribution k) {
 k.setStatus(PaymentStatus.SUCCESS);
 Campaign c = k.getCampaign();
 c.setRaised(c.getRaised() + k.getAmount());
 c.setBackersCount(c.getBackersCount() + 1);
 if (c.getFundingGoal() != null && c.getRaised() >= c.getFundingGoal()) {
   c.setStatus(CampaignStatus.GOAL_REACHED);
   campaignService.notify(c, "MILESTONE", "\"" + c.getTitle() + "\" reached its funding goal!");
 }
 campaigns.save(c);
 contributions.save(k);
}

private Campaign fundable(Long id) {
 Campaign c = campaignService.get(id);
 if (c.getStatus() != CampaignStatus.FUNDING) throw new ApiException(HttpStatus.CONFLICT, "This campaign is not open for funding");
 return c;
}

private double validAmount(Double a) {
 if (a == null || a < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum contribution is $1");
 if (a > 10000) throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum contribution is $10,000");
 return a;
}
}
