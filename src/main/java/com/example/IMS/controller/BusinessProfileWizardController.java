package com.example.IMS.controller;

import com.example.IMS.dto.AddBankAccountRequest;
import com.example.IMS.dto.BankDetailsDTO;
import com.example.IMS.dto.PaymentRequest;
import com.example.IMS.dto.PaymentResponse;
import com.example.IMS.dto.PaymentVerificationRequest;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.service.BankDetailsService;
import com.example.IMS.service.EmailService;
import com.example.IMS.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

/**
 * Handles the post-creation onboarding wizard steps:
 *   Step 1 (create) — handled by BusinessProfileController
 *   Step 2: Bank Details  GET/POST /business-profile/bank
 *   Step 3: Subscription   GET /business-profile/subscription
 *              + POST /business-profile/subscription/select
 *   Step 4: Review         GET /business-profile/review
 */
@Controller
@RequestMapping("/business-profile")
public class BusinessProfileWizardController {

    private static final Logger logger = LoggerFactory.getLogger(BusinessProfileWizardController.class);

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private BankDetailsService bankDetailsService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EmailService emailService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    /** Subscription plan prices in INR */
    private static final java.util.Map<String, Double> PLAN_PRICES =
            java.util.Map.of("CORE", 500.0, "PREMIUM", 2500.0);

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Bank Details
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/bank")
    public String bankForm(@RequestParam Long profileId, Model model) {
        User user = currentUser();
        BusinessProfile profile = businessProfileRepository
                .findByIdAndUserId(profileId, user.getId())
                .orElse(null);

        if (profile == null) {
            return "redirect:/business-profile/status";
        }

        List<BankDetailsDTO> bankAccounts =
                bankDetailsService.getBankAccountsByBusinessProfile(profileId, user.getId());

        model.addAttribute("profile", profile);
        model.addAttribute("profileId", profileId);
        model.addAttribute("bankAccounts", bankAccounts);
        model.addAttribute("bankRequest", new AddBankAccountRequest());
        return "business-profile/bank";
    }

    @PostMapping("/bank")
    public String saveBankDetails(
            @RequestParam Long profileId,
            @ModelAttribute("bankRequest") AddBankAccountRequest bankRequest,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {

        User user = currentUser();

        // Default isPrimary to false when unchecked (HTML checkbox sends nothing)
        if (bankRequest.getIsPrimary() == null) {
            bankRequest.setIsPrimary(false);
        }

        // Manually validate required fields (avoid @Valid to handle isPrimary null above)
        boolean hasError = false;
        if (bankRequest.getAccountHolderName() == null || bankRequest.getAccountHolderName().isBlank()) {
            result.rejectValue("accountHolderName", "NotBlank", "Account holder name is required");
            hasError = true;
        }
        if (bankRequest.getAccountNumber() == null || !bankRequest.getAccountNumber().matches("^[0-9]{9,18}$")) {
            result.rejectValue("accountNumber", "Pattern", "Enter a valid 9-18 digit account number");
            hasError = true;
        }
        if (bankRequest.getIfscCode() == null || !bankRequest.getIfscCode().matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            result.rejectValue("ifscCode", "Pattern", "Invalid IFSC code. Format: HDFC0001234");
            hasError = true;
        }
        if (bankRequest.getBankName() == null || bankRequest.getBankName().isBlank()) {
            result.rejectValue("bankName", "NotBlank", "Bank name is required");
            hasError = true;
        }

        if (hasError) {
            BusinessProfile profile = businessProfileRepository
                    .findByIdAndUserId(profileId, user.getId()).orElse(null);
            List<BankDetailsDTO> bankAccounts =
                    bankDetailsService.getBankAccountsByBusinessProfile(profileId, user.getId());
            model.addAttribute("profile", profile);
            model.addAttribute("profileId", profileId);
            model.addAttribute("bankAccounts", bankAccounts);
            return "business-profile/bank";
        }

        try {
            bankDetailsService.addBankAccount(profileId, bankRequest, user.getId());
            ra.addFlashAttribute("successMessage", "Bank account added successfully!");
        } catch (Exception e) {
            logger.error("Error adding bank account for profile {}", profileId, e);
            ra.addFlashAttribute("errorMessage", "Could not save bank account: " + e.getMessage());
            return "redirect:/business-profile/bank?profileId=" + profileId;
        }

        return "redirect:/business-profile/subscription?profileId=" + profileId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3 — Subscription
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/subscription")
    public String subscriptionPage(@RequestParam Long profileId, Model model) {
        User user = currentUser();
        BusinessProfile profile = businessProfileRepository
                .findByIdAndUserId(profileId, user.getId())
                .orElse(null);

        if (profile == null) {
            return "redirect:/business-profile/status";
        }

        model.addAttribute("profile", profile);
        model.addAttribute("profileId", profileId);
        return "business-profile/subscription";
    }

    /**
     * Step 3a: Plan selection — create a Razorpay order and redirect to checkout.
     * If the plan is unknown, fall through to review without charging.
     */
    @PostMapping("/subscription/select")
    public String selectPlan(
            @RequestParam Long profileId,
            @RequestParam(required = false) String plan,
            RedirectAttributes ra) {

        if (plan == null || !PLAN_PRICES.containsKey(plan)) {
            ra.addFlashAttribute("infoMessage", "No plan selected — you can subscribe later from your dashboard.");
            return "redirect:/business-profile/review?profileId=" + profileId;
        }

        User user = currentUser();

        // Block payment until business profile is verified by admin
        BusinessProfile profileCheck = businessProfileRepository
                .findByIdAndUserId(profileId, user.getId()).orElse(null);
        if (profileCheck == null || profileCheck.getVerificationStatus() != VerificationStatus.VERIFIED) {
            ra.addFlashAttribute("errorMessage",
                    "Your business profile must be verified by our team before purchasing a subscription. "
                    + "Please submit for verification first and wait for approval.");
            return "redirect:/business-profile/subscription?profileId=" + profileId;
        }

        double amount = PLAN_PRICES.get(plan);

        try {
            PaymentRequest payReq = new PaymentRequest(amount,
                    user.getFirstName() + " " + user.getLastName(),
                    user.getEmail());
            payReq.setDescription("FlowTrack " + plan + " Plan — 1 Month");
            payReq.setReceipt("SUB_" + profileId + "_" + plan + "_" + System.currentTimeMillis());

            PaymentResponse order = paymentService.createOrder(payReq);

            ra.addFlashAttribute("paymentOrder", order);
            ra.addFlashAttribute("planName", plan);
            ra.addFlashAttribute("profileId", profileId);
            return "redirect:/business-profile/subscription/pay?profileId=" + profileId + "&plan=" + plan;

        } catch (Exception e) {
            logger.error("Error creating Razorpay order for plan {} — {}", plan, e.getMessage());
            ra.addFlashAttribute("errorMessage",
                    "Payment gateway error. You can retry from your dashboard. " + e.getMessage());
            return "redirect:/business-profile/review?profileId=" + profileId;
        }
    }

    /** Step 3b: Show Razorpay checkout page */
    @GetMapping("/subscription/pay")
    public String showPaymentPage(
            @RequestParam Long profileId,
            @RequestParam String plan,
            Model model) {

        User user = currentUser();
        BusinessProfile profile = businessProfileRepository
                .findByIdAndUserId(profileId, user.getId()).orElse(null);
        if (profile == null) return "redirect:/business-profile/status";

        // paymentOrder comes via RedirectAttributes flash from selectPlan
        if (!model.containsAttribute("paymentOrder")) {
            // User navigated directly without selecting a plan — redirect back
            return "redirect:/business-profile/subscription?profileId=" + profileId;
        }

        model.addAttribute("profile", profile);
        model.addAttribute("profileId", profileId);
        model.addAttribute("planName", plan);
        model.addAttribute("planAmount", PLAN_PRICES.getOrDefault(plan, 0.0));
        model.addAttribute("razorpayKeyId", razorpayKeyId);
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("userEmail", user.getEmail());
        return "business-profile/subscription-pay";
    }

    /**
     * Step 3c: Razorpay calls back here after payment.
     * Verifies signature — on success, sends PE-01 email and proceeds to review.
     */
    @PostMapping("/subscription/verify")
    public String verifyPayment(
            @RequestParam Long profileId,
            @RequestParam String plan,
            @RequestParam String razorpayOrderId,
            @RequestParam String razorpayPaymentId,
            @RequestParam String razorpaySignature,
            RedirectAttributes ra) {

        PaymentVerificationRequest verReq = new PaymentVerificationRequest(
                razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if (!paymentService.verifyPaymentSignature(verReq)) {
            logger.warn("Subscription payment signature mismatch. orderId={}", razorpayOrderId);
            ra.addFlashAttribute("errorMessage",
                    "Payment verification failed. Please contact support with reference: " + razorpayOrderId);
            return "redirect:/business-profile/subscription?profileId=" + profileId;
        }

        logger.info("PE-01 subscription payment verified. plan={}, paymentId={}", plan, razorpayPaymentId);

        // Persist subscription state on the BusinessProfile
        User user = currentUser();
        BusinessProfile profile = businessProfileRepository
                .findByIdAndUserId(profileId, user.getId()).orElse(null);
        if (profile != null) {
            java.time.LocalDate now = java.time.LocalDate.now();
            profile.setSubscriptionPlan(plan);
            profile.setSubscriptionStartDate(now);
            profile.setSubscriptionExpiryDate(now.plusMonths(1));
            profile.setLastPaymentId(razorpayPaymentId);
            businessProfileRepository.save(profile);
            logger.info("Subscription persisted: plan={}, expiry={}", plan, profile.getSubscriptionExpiryDate());
        }

        // PE-01: send subscription activated email
        try {
            String validUntil = java.time.LocalDate.now().plusMonths(1)
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
            emailService.sendSubscriptionActivatedEmail(
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName(),
                    plan,
                    validUntil,
                    razorpayPaymentId);
        } catch (Exception mailEx) {
            logger.warn("PE-01 subscription email failed for {}: {}", user.getEmail(), mailEx.getMessage());
        }

        ra.addFlashAttribute("successMessage",
                plan + " plan activated! Payment ID: " + razorpayPaymentId);
        return "redirect:/business-profile/review?profileId=" + profileId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 4 — Review & Submit
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/review")
    public String reviewPage(@RequestParam Long profileId, Model model) {
        User user = currentUser();
        BusinessProfile profile = businessProfileRepository
                .findByIdAndUserId(profileId, user.getId())
                .orElse(null);

        if (profile == null) {
            return "redirect:/business-profile/status";
        }

        List<BankDetailsDTO> bankAccounts =
                bankDetailsService.getBankAccountsByBusinessProfile(profileId, user.getId());

        model.addAttribute("profile", profile);
        model.addAttribute("profileId", profileId);
        model.addAttribute("bankAccounts", bankAccounts);
        return "business-profile/review";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
