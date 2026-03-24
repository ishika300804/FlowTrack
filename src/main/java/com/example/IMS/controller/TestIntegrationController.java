package com.example.IMS.controller;

import com.example.IMS.dto.EmailRequest;
import com.example.IMS.dto.PaymentRequest;
import com.example.IMS.dto.PaymentResponse;
import com.example.IMS.dto.PaymentVerificationRequest;
import com.example.IMS.service.EmailService;
import com.example.IMS.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/test")
public class TestIntegrationController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private PaymentService paymentService;

    // ==================== EMAIL TEST ENDPOINTS ====================

    /**
     * Display email test page
     */
    @GetMapping("/email")
    public String showEmailTestPage(Model model) {
        model.addAttribute("emailRequest", new EmailRequest());
        return "test/email-test";
    }

    /**
     * Send test email - Simple Text
     */
    @PostMapping("/email/send-simple")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendSimpleEmail(@RequestBody @Valid EmailRequest emailRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            emailService.sendSimpleEmail(
                emailRequest.getTo(),
                emailRequest.getSubject(),
                emailRequest.getBody()
            );
            response.put("status", "success");
            response.put("message", "Simple email sent successfully to " + emailRequest.getTo());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send test email - HTML
     */
    @PostMapping("/email/send-html")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendHtmlEmail(@RequestBody @Valid EmailRequest emailRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            emailService.sendHtmlEmail(
                emailRequest.getTo(),
                emailRequest.getSubject(),
                emailRequest.getBody()
            );
            response.put("status", "success");
            response.put("message", "HTML email sent successfully to " + emailRequest.getTo());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send welcome email
     */
    @PostMapping("/email/send-welcome")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendWelcomeEmail(
            @RequestParam String email,
            @RequestParam String userName,
            @RequestParam String userType) {
        Map<String, Object> response = new HashMap<>();
        try {
            emailService.sendWelcomeEmail(email, userName, userType);
            response.put("status", "success");
            response.put("message", "Welcome email sent successfully to " + email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send welcome email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send OTP email
     */
    @PostMapping("/email/send-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendOtpEmail(
            @RequestParam String email,
            @RequestParam String otp) {
        Map<String, Object> response = new HashMap<>();
        try {
            emailService.sendOtpEmail(email, otp);
            response.put("status", "success");
            response.put("message", "OTP email sent successfully to " + email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send OTP email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== PAYMENT TEST ENDPOINTS ====================

    /**
     * Display payment test page
     */
    @GetMapping("/payment")
    public String showPaymentTestPage(Model model) {
        model.addAttribute("paymentRequest", new PaymentRequest());
        return "test/payment-test";
    }

    /**
     * Create test payment order
     */
    @PostMapping("/payment/create-order")
    @ResponseBody
    public ResponseEntity<PaymentResponse> createPaymentOrder(@RequestBody @Valid PaymentRequest paymentRequest) {
        try {
            System.out.println("Creating payment order for amount: " + paymentRequest.getAmount());
            PaymentResponse response = paymentService.createOrder(paymentRequest);
            if ("failed".equals(response.getStatus()) || "error".equals(response.getStatus())) {
                System.err.println("Payment order creation failed: " + response.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            System.out.println("Payment order created successfully: " + response.getOrderId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Exception in createPaymentOrder controller: " + e.getMessage());
            e.printStackTrace();
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to create payment order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify payment
     */
    @PostMapping("/payment/verify")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody PaymentVerificationRequest verificationRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isValid = paymentService.verifyPaymentSignature(verificationRequest);
            response.put("verified", isValid);
            response.put("status", isValid ? "success" : "failed");
            response.put("message", isValid ? "Payment verified successfully" : "Payment verification failed");
            
            // If payment is verified, send confirmation email
            if (isValid) {
                // You can add email notification here
                response.put("emailSent", false);
                response.put("emailMessage", "Email notification disabled for test");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("verified", false);
            response.put("status", "error");
            response.put("message", "Error verifying payment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Quick test endpoint - combines both email and payment
     */
    @GetMapping("/quick-test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("emailService", "ready");
        response.put("paymentService", "ready");
        response.put("message", "Email and Payment services are configured and ready for testing");
        response.put("instructions", Map.of(
            "emailTest", "POST /test/email/send-simple with EmailRequest body",
            "paymentTest", "POST /test/payment/create-order with PaymentRequest body"
        ));
        return ResponseEntity.ok(response);
    }
}
