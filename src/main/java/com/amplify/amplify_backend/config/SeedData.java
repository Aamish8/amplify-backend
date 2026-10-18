package com.amplify.amplify_backend.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.amplify.amplify_backend.model.AppUser;
import com.amplify.amplify_backend.model.Campaign;
import com.amplify.amplify_backend.model.CampaignStatus;
import com.amplify.amplify_backend.model.Notification;
import com.amplify.amplify_backend.repo.CampaignRepository;
import com.amplify.amplify_backend.repo.NotificationRepository;
import com.amplify.amplify_backend.repo.UserRepository;

import lombok.RequiredArgsConstructor;

//config/SeedData.java
@Component @RequiredArgsConstructor
public class SeedData implements CommandLineRunner {
private final UserRepository users;
private final CampaignRepository campaigns;
private final NotificationRepository notifications;

@Override public void run(String... args) {
 if (users.count() > 0) return;
 AppUser me = users.save(AppUser.builder().name("Demo User")
     .email("demo@amplify.app").avatarUrl("https://i.pravatar.cc/150?img=12").build());

 seed(me, "Clean Water for Rural Schools", "Health", "WaterAid Collective",
     "Help us spread awareness about clean water access in 200 villages.",
     "https://picsum.photos/seed/water/800/500", 5000.0, 3650.0, 142, CampaignStatus.FUNDING);
 seed(me, "Teach Kids to Code", "Education", "CodeBridge",
     "A message to bring free coding classes to underserved students.",
     "https://picsum.photos/seed/code/800/500", 3000.0, 3000.0, 98, CampaignStatus.PROMOTING);
 seed(me, "Save the Urban Sparrow", "Environment", "GreenCity Group",
     "Raise awareness about disappearing city birds and simple ways to help.",
     "https://picsum.photos/seed/bird/800/500", 2000.0, 420.0, 31, CampaignStatus.FUNDING);
 seed(me, "Mental Health Matters", "Health", "MindSpace",
     "Normalize talking about mental health in our communities.",
     "https://picsum.photos/seed/mind/800/500", 4000.0, 1800.0, 76, CampaignStatus.FUNDING);

 notifications.save(Notification.builder().user(me).type("MILESTONE")
     .message("Clean Water for Rural Schools reached 70% of its goal!").build());
}

private void seed(AppUser owner, String title, String cat, String creator, String summary,
                 String img, double goal, double raised, int backers, CampaignStatus st) {
 campaigns.save(Campaign.builder().owner(owner).title(title).category(cat)
     .creatorName(creator).summary(summary).coverImageUrl(img)
     .storyHeadline(title).storyIntro(summary + " Every contribution helps this message reach people who need to hear it.")
     .adHeadline(title).adDescription(summary).adCta("Learn More")
     .adBudget(goal * 0.85).durationDays(30).estimatedReach((long) (goal * 40))
     .fundingGoal(goal).raised(raised).backersCount(backers).status(st).currentStep(5)
     .fundingEndsAt(Instant.now().plus(21, ChronoUnit.DAYS)).build());
}
}
