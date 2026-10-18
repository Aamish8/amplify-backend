package com.amplify.amplify_backend.service;

import org.springframework.stereotype.Component;

import com.amplify.amplify_backend.dto.Dtos.Breakdown;
import com.amplify.amplify_backend.dto.Dtos.ReachEstimate;
import com.amplify.amplify_backend.model.Campaign;

//service/Pricing.java
@Component
public class Pricing {
static final double PLATFORM_RATE = 0.05, PROC_RATE = 0.029, PROC_FIXED = 0.30, REACH_PER_DOLLAR = 40;

double r2(double v) { return Math.round(v * 100) / 100.0; }

public int count(String csv) {
 return csv == null || csv.isBlank() ? 0 : csv.split(",").length;
}

public double reachPerDollar(Campaign c) {
 if (c.getEstimatedReach() != null && c.getAdBudget() != null && c.getAdBudget() > 0)
   return c.getEstimatedReach() / c.getAdBudget();
 return REACH_PER_DOLLAR;
}

public Breakdown breakdown(Campaign c, double amount) {
 double platform = r2(amount * PLATFORM_RATE);
 double ad = r2(amount - platform);
 double proc = r2(amount * PROC_RATE + PROC_FIXED);
 return new Breakdown(amount, platform, ad, proc, r2(amount + proc), Math.round(ad * reachPerDollar(c)));
}

public ReachEstimate reach(double budget, int locations, int interests) {
 double base = budget * REACH_PER_DOLLAR * (1 + 0.08 * Math.min(interests, 5)) * (1 + 0.05 * Math.min(locations, 5));
 return new ReachEstimate(Math.round(base * 0.8), Math.round(base * 1.2), Math.round(base));
}
}
