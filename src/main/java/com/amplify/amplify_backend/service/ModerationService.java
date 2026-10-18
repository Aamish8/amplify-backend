package com.amplify.amplify_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.amplify.amplify_backend.model.Campaign;
import com.amplify.amplify_backend.model.CampaignStatus;

import lombok.extern.slf4j.Slf4j;

//service/ModerationService.java
@Service @Slf4j
public class ModerationService {
public record Result(CampaignStatus status, String feedback) {}
private static final List<String> BANNED = List.of("guaranteed returns", "get rich", "miracle cure", "100% guaranteed");
private final ChatClient chat;

public ModerationService(ObjectProvider<ChatClient.Builder> builder) {
 ChatClient.Builder b = builder.getIfAvailable();
 this.chat = b == null ? null : b.build();
}

public Result review(Campaign c) {
 String text = String.join(" ", nz(c.getTitle()), nz(c.getSummary()), nz(c.getStoryIntro()), nz(c.getAdDescription())).toLowerCase();
 for (String w : BANNED)
   if (text.contains(w)) return new Result(CampaignStatus.REJECTED, "Contains prohibited claim: \"" + w + "\". Campaigns cannot make unverifiable promises.");

 List<String> issues = new ArrayList<>();
 if (nz(c.getTitle()).length() < 8) issues.add("Title is too short. Make it clear and specific.");
 if (nz(c.getSummary()).length() < 30) issues.add("Summary needs at least 30 characters.");
 if (nz(c.getStoryIntro()).length() < 50) issues.add("Story intro is too thin. Explain why this message matters.");
 if (nz(c.getSources()).length() < 5) issues.add("Add at least one supporting source (title + URL).");
 if (nz(c.getCoverImageUrl()).isBlank()) issues.add("Add a cover image.");
 if (nz(c.getAdHeadline()).isBlank() || nz(c.getAdCta()).isBlank()) issues.add("Ad headline and CTA text are required.");
 if (c.getAdBudget() == null || c.getAdBudget() <= 0) issues.add("Set an advertising budget in the Reach step.");
 if (!issues.isEmpty()) return new Result(CampaignStatus.CHANGES_REQUESTED, "- " + String.join("\n- ", issues));

 return ai(c);
}

private Result ai(Campaign c) {
 if (chat == null) return new Result(CampaignStatus.APPROVED, "Approved by automated checks.");
 try {
   String prompt = """
     You are a content moderator for a campaign amplification platform. Review the campaign.
     Reply with ONE first line: APPROVED, CHANGES_REQUESTED or REJECTED.
     Then 1-2 short sentences of feedback. Reject only misleading, hateful or scam content.
     Title: %s
     Summary: %s
     Story: %s
     Ad: %s - %s""".formatted(c.getTitle(), c.getSummary(), c.getStoryIntro(), c.getAdHeadline(), c.getAdDescription());
   String out = chat.prompt().user(prompt).call().content();
   String first = out.trim().split("\n", 2)[0].toUpperCase();
   String feedback = out.contains("\n") ? out.substring(out.indexOf('\n')).trim() : "Reviewed by AI.";
   if (first.contains("REJECTED")) return new Result(CampaignStatus.REJECTED, feedback);
   if (first.contains("CHANGES")) return new Result(CampaignStatus.CHANGES_REQUESTED, feedback);
   return new Result(CampaignStatus.APPROVED, feedback);
 } catch (Exception e) {
   log.warn("AI unavailable, using rule-based result: {}", e.getMessage());
   return new Result(CampaignStatus.APPROVED, "Approved by automated checks.");
 }
}

private static String nz(String s) { return s == null ? "" : s; }
}
