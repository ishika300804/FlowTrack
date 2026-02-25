package com.example.IMS.interceptor;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.OnboardingStage;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.service.FeatureToggleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Phase 2: Soft Enforcement Interceptor
 * 
 * Shows non-blocking banners/notifications to encourage onboarding completion:
 * - "Complete your business profile to unlock all features"
 * - "Verify your business to access financial operations"
 * 
 * Banners can be dismissed but will reappear after session expiry
 * Tracks dismissal count for analytics
 */
@Component
public class OnboardingBannerInterceptor implements HandlerInterceptor {

    @Autowired
    private FeatureToggleService featureToggleService;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    private static final String BANNER_DISMISSED_KEY = "onboarding_banner_dismissed";
    private static final String BANNER_DISMISS_COUNT_KEY = "onboarding_banner_dismiss_count";

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        
        // Skip if soft enforcement is disabled
        if (!featureToggleService.isSoftEnforcementEnabled()) {
            return;
        }

        // Skip if no ModelAndView (e.g., REST endpoints)
        if (modelAndView == null) {
            return;
        }

        // Skip for redirects
        if (modelAndView.getViewName() != null && modelAndView.getViewName().startsWith("redirect:")) {
            return;
        }

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            return;
        }

        User user = (User) principal;
        HttpSession session = request.getSession();

        // Check if banner was dismissed in this session
        Boolean dismissed = (Boolean) session.getAttribute(BANNER_DISMISSED_KEY);
        if (Boolean.TRUE.equals(dismissed)) {
            return;
        }

        // Check business profile status (findByUserId returns List)
        Optional<BusinessProfile> profileOpt = businessProfileRepository.findByUserId(user.getId())
            .stream().findFirst();
        
        OnboardingBannerData bannerData = new OnboardingBannerData();
        
        if (profiles.isEmpty()) {
            // No profile - show creation banner
            bannerData.setShow(true);
            bannerData.setType("warning");
            bannerData.setTitle("Complete Your Business Profile");
            bannerData.setMessage("Create your business profile to unlock all FlowTrack features.");
            bannerData.setActionUrl("/business-profile/create");
            bannerData.setActionText("Create Profile");
        } else {
            BusinessProfile profile = profiles.get(0); // Use first profile
            
            if (profile.getVerificationStatus() == VerificationStatus.DRAFT) {
                // Draft profile - encourage submission
                bannerData.setShow(true);
                bannerData.setType("info");
                bannerData.setTitle("Submit Your Profile for Verification");
                bannerData.setMessage("Your business profile is in draft. Submit it for verification to access all features.");
                bannerData.setActionUrl("/business-profile/submit");
                bannerData.setActionText("Submit for Verification");
                
            } else if (profile.getVerificationStatus() == VerificationStatus.PENDING) {
                // Pending verification - informational
                bannerData.setShow(true);
                bannerData.setType("info");
                bannerData.setTitle("Verification in Progress");
                bannerData.setMessage("Your business profile is being verified. This usually takes 24-48 hours.");
                bannerData.setActionUrl("/business-profile/status");
                bannerData.setActionText("Check Status");
                
            } else if (profile.getVerificationStatus() == VerificationStatus.REJECTED) {
                // Rejected - action required
                bannerData.setShow(true);
                bannerData.setType("danger");
                bannerData.setTitle("Verification Failed");
                bannerData.setMessage("Your business profile verification was rejected. Please review and resubmit.");
                bannerData.setActionUrl("/business-profile/edit");
                bannerData.setActionText("Review & Resubmit");
                
            } else if (profile.getVerificationStatus() == VerificationStatus.VERIFIED && 
                       profile.getOnboardingStage() != OnboardingStage.ACTIVE) {
                // Verified but not active - complete remaining steps
                bannerData.setShow(true);
                bannerData.setType("warning");
                bannerData.setTitle("Complete Onboarding");
                bannerData.setMessage("Your profile is verified! Complete the remaining steps to activate your account.");
                bannerData.setActionUrl("/onboarding/complete");
                bannerData.setActionText("Complete Onboarding");
            }
        }

        // Add banner data to model
        if (bannerData.isShow()) {
            modelAndView.addObject("onboardingBanner", bannerData);
            
            // Track dismissal count for analytics
            Integer dismissCount = (Integer) session.getAttribute(BANNER_DISMISS_COUNT_KEY);
            if (dismissCount == null) {
                dismissCount = 0;
            }
            modelAndView.addObject("bannerDismissCount", dismissCount);
        }
    }

    /**
     * Banner data model for Thymeleaf templates
     */
    public static class OnboardingBannerData {
        private boolean show;
        private String type; // success, info, warning, danger
        private String title;
        private String message;
        private String actionUrl;
        private String actionText;

        public boolean isShow() { return show; }
        public void setShow(boolean show) { this.show = show; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
        
        public String getActionText() { return actionText; }
        public void setActionText(String actionText) { this.actionText = actionText; }
    }
}
