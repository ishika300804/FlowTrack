package com.example.IMS.controller;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.repository.BusinessProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/vendor")
public class VendorDashboardController {

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = currentUser();
        List<BusinessProfile> profiles = businessProfileRepository.findByUserId(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("profiles", profiles);
        model.addAttribute("hasProfile", !profiles.isEmpty());

        if (!profiles.isEmpty()) {
            BusinessProfile primary = profiles.get(0);
            model.addAttribute("primaryProfile", primary);
            model.addAttribute("profileStatus", primary.getVerificationStatus());
            model.addAttribute("isVerified",
                    primary.getVerificationStatus() != null &&
                    primary.getVerificationStatus().name().equals("VERIFIED"));
        }

        return "vendor/dashboard";
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}
