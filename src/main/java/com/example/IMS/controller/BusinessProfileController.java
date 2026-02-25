package com.example.IMS.controller;

import com.example.IMS.dto.BusinessProfileDTO;
import com.example.IMS.dto.CreateBusinessProfileRequest;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.service.BusinessProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

/**
 * Business Profile Flow Controller
 *
 * Handles the full business profile lifecycle for regular users:
 *  - Create a new business profile (DRAFT)
 *  - View profile status
 *  - Edit a DRAFT / REJECTED profile
 *  - Submit for verification (DRAFT → PENDING)
 */
@Controller
@RequestMapping("/business-profile")
public class BusinessProfileController {

    private static final Logger logger = LoggerFactory.getLogger(BusinessProfileController.class);

    @Autowired
    private BusinessProfileService businessProfileService;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("profileRequest", new CreateBusinessProfileRequest());
        model.addAttribute("businessTypes", BusinessType.values());
        return "business-profile/create";
    }

    @PostMapping("/create")
    public String createProfile(
            @Valid @ModelAttribute("profileRequest") CreateBusinessProfileRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("businessTypes", BusinessType.values());
            return "business-profile/create";
        }

        try {
            User currentUser = currentUser();
            BusinessProfileDTO created = businessProfileService.createBusinessProfile(request, currentUser.getId());
            logger.info("Business profile created for user {}: profileId={}", currentUser.getId(), created.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Business profile created! Submit it for verification when you are ready.");
            return "redirect:/business-profile/status";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("businessTypes", BusinessType.values());
            return "business-profile/create";
        } catch (Exception e) {
            logger.error("Error creating business profile", e);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            model.addAttribute("businessTypes", BusinessType.values());
            return "business-profile/create";
        }
    }

    // -----------------------------------------------------------------------
    // STATUS
    // -----------------------------------------------------------------------

    @GetMapping("/status")
    public String viewStatus(Model model) {
        User currentUser = currentUser();
        List<BusinessProfile> profiles = businessProfileRepository.findByUserId(currentUser.getId());
        model.addAttribute("profiles", profiles);
        model.addAttribute("hasProfile", !profiles.isEmpty());
        return "business-profile/status";
    }

    // -----------------------------------------------------------------------
    // EDIT
    // -----------------------------------------------------------------------

    @GetMapping("/edit")
    public String showEditForm(Model model,
                               @RequestParam(required = false) Long profileId) {
        User currentUser = currentUser();
        List<BusinessProfile> profiles = businessProfileRepository.findByUserId(currentUser.getId());

        BusinessProfile profile = resolveProfile(profiles, profileId);
        if (profile == null) {
            return "redirect:/business-profile/create";
        }

        if (profile.getVerificationStatus() == VerificationStatus.VERIFIED ||
                profile.getVerificationStatus() == VerificationStatus.PENDING) {
            model.addAttribute("errorMessage",
                    "You cannot edit a profile that is pending or already verified. "
                            + "Contact support if you need to make changes.");
            return "business-profile/status";
        }

        // Map entity → form request
        CreateBusinessProfileRequest request = mapToRequest(profile);
        model.addAttribute("profileRequest", request);
        model.addAttribute("profileId", profile.getId());
        model.addAttribute("businessTypes", BusinessType.values());
        return "business-profile/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
            @Valid @ModelAttribute("profileRequest") CreateBusinessProfileRequest request,
            BindingResult result,
            @RequestParam Long profileId,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("profileId", profileId);
            model.addAttribute("businessTypes", BusinessType.values());
            return "business-profile/edit";
        }

        try {
            User currentUser = currentUser();
            businessProfileService.updateBusinessProfile(profileId, request, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
            return "redirect:/business-profile/status";
        } catch (Exception e) {
            logger.error("Error updating business profile {}", profileId, e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("profileId", profileId);
            model.addAttribute("businessTypes", BusinessType.values());
            return "business-profile/edit";
        }
    }

    // -----------------------------------------------------------------------
    // SUBMIT FOR VERIFICATION
    // -----------------------------------------------------------------------

    @GetMapping("/submit")
    public String showSubmitConfirmation(Model model,
                                         @RequestParam(required = false) Long profileId) {
        User currentUser = currentUser();
        List<BusinessProfile> profiles = businessProfileRepository.findByUserId(currentUser.getId());
        BusinessProfile profile = resolveProfile(profiles, profileId);

        if (profile == null) {
            return "redirect:/business-profile/create";
        }

        if (profile.getVerificationStatus() != VerificationStatus.DRAFT &&
                profile.getVerificationStatus() != VerificationStatus.REJECTED) {
            return "redirect:/business-profile/status";
        }

        model.addAttribute("profile", profile);
        return "business-profile/submit";
    }

    @PostMapping("/submit")
    public String submitProfile(@RequestParam Long profileId,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = currentUser();
            businessProfileService.submitForVerification(profileId, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your business profile has been submitted for verification. "
                            + "We will review it within 24–48 hours.");
            return "redirect:/business-profile/status";
        } catch (Exception e) {
            logger.error("Error submitting profile {} for verification", profileId, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/business-profile/submit?profileId=" + profileId;
        }
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    /** Pick the requested profileId, or fall back to the first profile in the list. */
    private BusinessProfile resolveProfile(List<BusinessProfile> profiles, Long profileId) {
        if (profiles.isEmpty()) return null;
        if (profileId != null) {
            return profiles.stream()
                    .filter(p -> p.getId().equals(profileId))
                    .findFirst()
                    .orElse(profiles.get(0));
        }
        return profiles.get(0);
    }

    private CreateBusinessProfileRequest mapToRequest(BusinessProfile profile) {
        CreateBusinessProfileRequest req = new CreateBusinessProfileRequest();
        req.setLegalBusinessName(profile.getLegalBusinessName());
        req.setBusinessType(profile.getBusinessType());
        req.setGstin(profile.getGstin());
        req.setPanNumber(profile.getPanNumber());
        req.setCinNumber(profile.getCinNumber());
        req.setUdyamNumber(profile.getUdyamNumber());
        req.setRegisteredAddress(profile.getRegisteredAddress());
        req.setState(profile.getState());
        req.setPincode(profile.getPincode());
        return req;
    }
}
