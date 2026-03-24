package com.example.IMS.controller;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.OnboardingStage;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.IUserRepository;
import com.example.IMS.service.EmailService;
import com.example.IMS.service.FeatureToggleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 4: Admin Dashboard for Onboarding Management
 * 
 * Provides platform admins with:
 * - Legacy user report (users without business profiles)
 * - Onboarding status dashboard (conversion funnel)
 * - Manual approval interface
 * - Temporary exemption management
 */
@Controller
@RequestMapping("/admin/onboarding")
@PreAuthorize("hasAuthority('ROLE_PLATFORM_ADMIN')")
public class OnboardingAdminController {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private FeatureToggleService featureToggleService;

    @Autowired
    private EmailService emailService;

    /**
     * Dashboard: Onboarding overview with metrics
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        OnboardingMetrics metrics = calculateMetrics();
        
        model.addAttribute("metrics", metrics);
        model.addAttribute("featureFlags", getFeatureFlagStatus());
        
        return "admin/onboarding-dashboard";
    }

    /**
     * Legacy Users Report: Users without business profiles
     */
    @GetMapping("/legacy-users")
    public String legacyUsers(Model model) {
        List<User> allUsers = userRepository.findAll();
        List<BusinessProfile> allProfiles = businessProfileRepository.findAll();
        
        Set<Long> usersWithProfiles = allProfiles.stream()
            .map(profile -> profile.getUser().getId())
            .collect(Collectors.toSet());
        
        List<LegacyUserInfo> legacyUsers = allUsers.stream()
            .filter(user -> !usersWithProfiles.contains(user.getId()))
            .filter(user -> !user.getRoles().stream()
                .anyMatch(role -> "ROLE_PLATFORM_ADMIN".equals(role.getName())))
            .map(user -> {
                LegacyUserInfo info = new LegacyUserInfo();
                info.setUserId(user.getId());
                info.setUsername(user.getUsername());
                info.setEmail(user.getEmail());
                info.setRoles(user.getRoles().stream()
                    .map(role -> role.getName().replace("ROLE_", ""))
                    .collect(Collectors.joining(", ")));
                // Calculate days since registration (if available)
                // For now, set as N/A
                info.setDaysSinceRegistration("N/A");
                return info;
            })
            .collect(Collectors.toList());
        
        model.addAttribute("legacyUsers", legacyUsers);
        model.addAttribute("totalLegacyUsers", legacyUsers.size());
        
        return "admin/legacy-users";
    }

    /**
     * Onboarding Status Report: Conversion funnel
     */
    @GetMapping("/status-report")
    public String statusReport(Model model) {
        List<BusinessProfile> allProfiles = businessProfileRepository.findAll();
        
        Map<String, Long> statusBreakdown = new HashMap<>();
        statusBreakdown.put("DRAFT", allProfiles.stream()
            .filter(p -> p.getVerificationStatus() == VerificationStatus.DRAFT).count());
        statusBreakdown.put("PENDING", allProfiles.stream()
            .filter(p -> p.getVerificationStatus() == VerificationStatus.PENDING).count());
        statusBreakdown.put("VERIFIED", allProfiles.stream()
            .filter(p -> p.getVerificationStatus() == VerificationStatus.VERIFIED).count());
        statusBreakdown.put("REJECTED", allProfiles.stream()
            .filter(p -> p.getVerificationStatus() == VerificationStatus.REJECTED).count());
        
        Map<String, Long> stageBreakdown = new HashMap<>();
        stageBreakdown.put("TIER1_COMPLETE", allProfiles.stream()
            .filter(p -> p.getOnboardingStage() == OnboardingStage.TIER1_COMPLETE).count());
        stageBreakdown.put("TIER2_COMPLETE", allProfiles.stream()
            .filter(p -> p.getOnboardingStage() == OnboardingStage.TIER2_COMPLETE).count());
        stageBreakdown.put("ACTIVE", allProfiles.stream()
            .filter(p -> p.getOnboardingStage() == OnboardingStage.ACTIVE).count());
        
        model.addAttribute("statusBreakdown", statusBreakdown);
        model.addAttribute("stageBreakdown", stageBreakdown);
        model.addAttribute("totalProfiles", allProfiles.size());
        
        return "admin/onboarding-status-report";
    }

    /**
     * Pending Approvals: Profiles awaiting manual review
     */
    @GetMapping("/pending-approvals")
    public String pendingApprovals(Model model) {
        List<BusinessProfile> pendingProfiles = businessProfileRepository.findAll().stream()
            .filter(p -> p.getVerificationStatus() == VerificationStatus.PENDING)
            .collect(Collectors.toList());
        
        model.addAttribute("pendingProfiles", pendingProfiles);
        model.addAttribute("totalPending", pendingProfiles.size());
        
        return "admin/pending-approvals";
    }

    /**
     * Approve a business profile
     */
    @PostMapping("/approve/{profileId}")
    @ResponseBody
    public Map<String, Object> approveProfile(@PathVariable Long profileId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<BusinessProfile> profileOpt = businessProfileRepository.findById(profileId);
        if (!profileOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Profile not found");
            return response;
        }
        
        BusinessProfile profile = profileOpt.get();
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        profile.setOnboardingStage(OnboardingStage.ACTIVE);
        profile.setUpdatedAt(LocalDateTime.now());
        businessProfileRepository.save(profile);

        // E-07/E-11: notify user — approved and fully active
        try {
            User user = profile.getUser();
            emailService.sendProfileApprovedEmail(
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName());
        } catch (Exception mailEx) {
            // Log but don't fail the API response
            System.err.println("E-07 email failed: " + mailEx.getMessage());
        }

        response.put("success", true);
        response.put("message", "Profile approved successfully");
        return response;
    }

    /**
     * Reject a business profile
     */
    @PostMapping("/reject/{profileId}")
    @ResponseBody
    public Map<String, Object> rejectProfile(@PathVariable Long profileId, 
                                             @RequestParam String reason) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<BusinessProfile> profileOpt = businessProfileRepository.findById(profileId);
        if (!profileOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Profile not found");
            return response;
        }
        
        BusinessProfile profile = profileOpt.get();
        profile.setVerificationStatus(VerificationStatus.REJECTED);
        profile.setRejectionReason(reason);
        profile.setUpdatedAt(LocalDateTime.now());
        businessProfileRepository.save(profile);

        // E-08: notify user — rejected with reason
        try {
            User user = profile.getUser();
            emailService.sendProfileRejectedEmail(
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName(),
                    reason);
        } catch (Exception mailEx) {
            System.err.println("E-08 email failed: " + mailEx.getMessage());
        }

        response.put("success", true);
        response.put("message", "Profile rejected");
        return response;
    }

    /**
     * Calculate onboarding metrics
     */
    private OnboardingMetrics calculateMetrics() {
        OnboardingMetrics metrics = new OnboardingMetrics();
        
        long totalUsers = userRepository.count();
        List<BusinessProfile> allProfiles = businessProfileRepository.findAll();
        long totalProfiles = allProfiles.size();
        long activeProfiles = allProfiles.stream()
            .filter(p -> p.getOnboardingStage() == OnboardingStage.ACTIVE).count();
        long pendingProfiles = allProfiles.stream()
            .filter(p -> p.getVerificationStatus() == VerificationStatus.PENDING).count();
        
        metrics.setTotalUsers(totalUsers);
        metrics.setTotalProfiles(totalProfiles);
        metrics.setActiveProfiles(activeProfiles);
        metrics.setPendingProfiles(pendingProfiles);
        metrics.setLegacyUsers(totalUsers - totalProfiles);
        
        // Calculate conversion rate
        if (totalUsers > 0) {
            metrics.setConversionRate((double) totalProfiles / totalUsers * 100);
            metrics.setActivationRate((double) activeProfiles / totalUsers * 100);
        }
        
        return metrics;
    }

    /**
     * Get feature flag status
     */
    private Map<String, Boolean> getFeatureFlagStatus() {
        Map<String, Boolean> flags = new HashMap<>();
        flags.put("onboardingEnabled", featureToggleService.isOnboardingEnabled());
        flags.put("softEnforcement", featureToggleService.isSoftEnforcementEnabled());
        flags.put("hardEnforcement", featureToggleService.isHardEnforcementEnabled());
        flags.put("adminOverride", featureToggleService.isAdminOverrideEnabled());
        return flags;
    }

    // DTOs
    public static class OnboardingMetrics {
        private long totalUsers;
        private long totalProfiles;
        private long activeProfiles;
        private long pendingProfiles;
        private long legacyUsers;
        private double conversionRate;
        private double activationRate;

        // Getters and setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        
        public long getTotalProfiles() { return totalProfiles; }
        public void setTotalProfiles(long totalProfiles) { this.totalProfiles = totalProfiles; }
        
        public long getActiveProfiles() { return activeProfiles; }
        public void setActiveProfiles(long activeProfiles) { this.activeProfiles = activeProfiles; }
        
        public long getPendingProfiles() { return pendingProfiles; }
        public void setPendingProfiles(long pendingProfiles) { this.pendingProfiles = pendingProfiles; }
        
        public long getLegacyUsers() { return legacyUsers; }
        public void setLegacyUsers(long legacyUsers) { this.legacyUsers = legacyUsers; }
        
        public double getConversionRate() { return conversionRate; }
        public void setConversionRate(double conversionRate) { this.conversionRate = conversionRate; }
        
        public double getActivationRate() { return activationRate; }
        public void setActivationRate(double activationRate) { this.activationRate = activationRate; }
    }

    public static class LegacyUserInfo {
        private Long userId;
        private String username;
        private String email;
        private String roles;
        private String daysSinceRegistration;

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getRoles() { return roles; }
        public void setRoles(String roles) { this.roles = roles; }
        
        public String getDaysSinceRegistration() { return daysSinceRegistration; }
        public void setDaysSinceRegistration(String daysSinceRegistration) { 
            this.daysSinceRegistration = daysSinceRegistration; 
        }
    }
}
