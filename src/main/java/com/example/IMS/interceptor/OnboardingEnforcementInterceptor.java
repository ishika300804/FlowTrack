package com.example.IMS.interceptor;

import com.example.IMS.context.ActiveBusinessProfileContext;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Phase 3: Hard Enforcement Interceptor
 * 
 * Blocks access to protected features if:
 * - User has no business profile
 * - Business profile verification_status != VERIFIED
 * - Business profile onboarding_stage != ACTIVE
 * 
 * Exemptions:
 * - Grace period not expired
 * - Admin override granted
 * - Whitelisted paths (login, registration, onboarding pages)
 */
@Component
public class OnboardingEnforcementInterceptor implements HandlerInterceptor {

    @Autowired
    private FeatureToggleService featureToggleService;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private ActiveBusinessProfileContext activeBusinessProfileContext;

    // Paths that require ACTIVE onboarding
    private static final List<String> PROTECTED_INVENTORY_PATHS = Arrays.asList(
        "/items/add",
        "/items/edit",
        "/items/delete",
        "/vendors/add",
        "/vendors/edit",
        "/vendors/delete",
        "/loans/issue",
        "/loans/return"
    );

    // Paths that require VERIFIED status (financial operations)
    private static final List<String> PROTECTED_FINANCIAL_PATHS = Arrays.asList(
        "/payments/initiate",
        "/payments/settle",
        "/investments/request",
        "/investments/accept"
    );

    // Paths that are always accessible (whitelisted)
    private static final List<String> WHITELISTED_PATHS = Arrays.asList(
        "/login",
        "/register",
        "/logout",
        "/onboarding",
        "/business-profile",
        "/verification",
        "/static",
        "/css",
        "/js",
        "/images",
        "/api/chatbot"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Skip if hard enforcement is disabled
        if (!featureToggleService.isHardEnforcementEnabled()) {
            return true;
        }

        String requestPath = request.getRequestURI();

        // Allow whitelisted paths
        if (isWhitelisted(requestPath)) {
            return true;
        }

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true; // Let Spring Security handle authentication
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            return true;
        }

        User user = (User) principal;

        // Check if user has business profile
        Optional<BusinessProfile> profileOpt = businessProfileRepository.findByUserId(user.getId());
        
        if (!profileOpt.isPresent()) {
            // No business profile - block protected operations
            if (requiresOnboarding(requestPath)) {
                response.sendRedirect("/onboarding/required?reason=no_profile");
                return false;
            }
            return true;
        }

        BusinessProfile profile = profileOpt.get();

        // Check grace period
        if (isWithinGracePeriod(profile)) {
            return true; // Allow during grace period
        }

        // Check admin override
        if (hasAdminOverride(user)) {
            return true;
        }

        // Enforce inventory operations require ACTIVE stage
        if (PROTECTED_INVENTORY_PATHS.stream().anyMatch(requestPath::startsWith)) {
            if (profile.getOnboardingStage() != OnboardingStage.ACTIVE) {
                response.sendRedirect("/onboarding/required?reason=not_active&stage=" + profile.getOnboardingStage());
                return false;
            }
        }

        // Enforce financial operations require VERIFIED status
        if (PROTECTED_FINANCIAL_PATHS.stream().anyMatch(requestPath::startsWith)) {
            if (profile.getVerificationStatus() != VerificationStatus.VERIFIED) {
                response.sendRedirect("/onboarding/required?reason=not_verified&status=" + profile.getVerificationStatus());
                return false;
            }
        }

        return true;
    }

    private boolean isWhitelisted(String path) {
        return WHITELISTED_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean requiresOnboarding(String path) {
        return PROTECTED_INVENTORY_PATHS.stream().anyMatch(path::startsWith) ||
               PROTECTED_FINANCIAL_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isWithinGracePeriod(BusinessProfile profile) {
        if (profile.getCreatedAt() == null) {
            return false;
        }
        
        long daysSinceCreation = ChronoUnit.DAYS.between(profile.getCreatedAt(), LocalDateTime.now());
        return daysSinceCreation < featureToggleService.getGracePeriodDays();
    }

    private boolean hasAdminOverride(User user) {
        if (!featureToggleService.isAdminOverrideEnabled()) {
            return false;
        }
        
        // Check if user has PLATFORM_ADMIN role
        return user.getRoles().stream()
            .anyMatch(role -> "ROLE_PLATFORM_ADMIN".equals(role.getName()));
    }
}
