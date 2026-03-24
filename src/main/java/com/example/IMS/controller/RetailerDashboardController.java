package com.example.IMS.controller;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/retailer")
public class RetailerDashboardController {

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = currentUser();
        List<BusinessProfile> profiles = businessProfileRepository.findByUserId(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("profiles", profiles);
        model.addAttribute("hasProfile", !profiles.isEmpty());

        // Pick primary/first profile for quick-access widgets
        if (!profiles.isEmpty()) {
            BusinessProfile primary = profiles.get(0);
            model.addAttribute("primaryProfile", primary);
            model.addAttribute("profileStatus", primary.getVerificationStatus());
            model.addAttribute("onboardingStage", primary.getOnboardingStage());
            model.addAttribute("isVerified",
                    primary.getVerificationStatus() != null &&
                    primary.getVerificationStatus().name().equals("VERIFIED"));
            model.addAttribute("isActive",
                    primary.getOnboardingStage() != null &&
                    primary.getOnboardingStage().name().equals("ACTIVE"));

            // Subscription info
            model.addAttribute("subscriptionPlan",    primary.getSubscriptionPlan());
            model.addAttribute("subscriptionExpiry",  primary.getSubscriptionExpiryDate());
            model.addAttribute("hasSubscription",     primary.hasActiveSubscription());
        } else {
            model.addAttribute("hasSubscription", false);
        }

        // Analytics stats
        AnalyticsService.RetailerStats stats = analyticsService.getRetailerStats();
        model.addAttribute("stats", stats);

        return "retailer/dashboard";
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}

