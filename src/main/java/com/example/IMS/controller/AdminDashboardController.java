package com.example.IMS.controller;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.OnboardingStage;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Platform Admin main dashboard.
 * Reachable at /admin/dashboard — the URL SecurityConfig redirects to after admin login.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_PLATFORM_ADMIN')")
public class AdminDashboardController {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<BusinessProfile> allProfiles = businessProfileRepository.findAll();

        long totalUsers      = userRepository.count();
        long totalProfiles   = allProfiles.size();
        long pendingCount    = allProfiles.stream()
                .filter(p -> p.getVerificationStatus() == VerificationStatus.PENDING).count();
        long verifiedCount   = allProfiles.stream()
                .filter(p -> p.getVerificationStatus() == VerificationStatus.VERIFIED).count();
        long rejectedCount   = allProfiles.stream()
                .filter(p -> p.getVerificationStatus() == VerificationStatus.REJECTED).count();
        long activeCount     = allProfiles.stream()
                .filter(p -> p.getOnboardingStage() == OnboardingStage.ACTIVE).count();

        // Legacy users: registered users without any business profile
        long usersWithProfileCount = allProfiles.stream()
                .map(p -> p.getUser().getId()).distinct().count();
        long legacyUsers = totalUsers - usersWithProfileCount;

        User admin = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        model.addAttribute("adminName", admin.getFirstName() + " " + admin.getLastName());
        model.addAttribute("totalUsers",    totalUsers);
        model.addAttribute("totalProfiles", totalProfiles);
        model.addAttribute("pendingCount",  pendingCount);
        model.addAttribute("verifiedCount", verifiedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("activeCount",   activeCount);
        model.addAttribute("legacyUsers",   legacyUsers);

        return "admin/dashboard";
    }
}
