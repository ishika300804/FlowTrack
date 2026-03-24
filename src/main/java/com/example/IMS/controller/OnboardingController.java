package com.example.IMS.controller;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.repository.BusinessProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Onboarding Flow Controller
 *
 * Handles informational / gating pages for the staged rollout:
 *  - /onboarding/required   → shown by hard-enforcement interceptor when a user must complete onboarding
 *  - /onboarding/complete   → shown after a user's profile becomes ACTIVE
 *  - /onboarding/banner/dismiss → AJAX endpoint to dismiss the soft-enforcement banner for the session
 */
@Controller
@RequestMapping("/onboarding")
public class OnboardingController {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingController.class);

    private static final String BANNER_DISMISSED_KEY = "onboarding_banner_dismissed";
    private static final String BANNER_DISMISS_COUNT_KEY = "onboarding_banner_dismiss_count";

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    /**
     * Enforcement gate — hard-enforcement interceptor redirects here when a user
     * tries to access a protected resource without an ACTIVE / VERIFIED profile.
     */
    @GetMapping("/required")
    public String onboardingRequired(Model model) {
        User currentUser = currentUser();
        List<BusinessProfile> profiles = businessProfileRepository.findByUserId(currentUser.getId());

        model.addAttribute("hasProfile", !profiles.isEmpty());
        model.addAttribute("profiles", profiles);
        return "onboarding/required";
    }

    /**
     * Completion celebration page — shown when profile reaches ACTIVE stage.
     */
    @GetMapping("/complete")
    public String onboardingComplete(Model model) {
        User currentUser = currentUser();
        List<BusinessProfile> profiles = businessProfileRepository.findByUserId(currentUser.getId());

        model.addAttribute("profiles", profiles);
        return "onboarding/complete";
    }

    /**
     * Dismiss the soft-enforcement banner for the current session.
     * Increments the dismiss counter for analytics and sets the dismissed flag.
     * Returns a JSON response so the banner can hide itself via JavaScript.
     */
    @PostMapping(value = "/banner/dismiss", produces = "application/json")
    @ResponseBody
    public String dismissBanner(HttpSession session) {
        session.setAttribute(BANNER_DISMISSED_KEY, Boolean.TRUE);

        Integer count = (Integer) session.getAttribute(BANNER_DISMISS_COUNT_KEY);
        count = (count == null) ? 1 : count + 1;
        session.setAttribute(BANNER_DISMISS_COUNT_KEY, count);

        logger.debug("Onboarding banner dismissed (count={})", count);
        return "{\"dismissed\":true}";
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}
