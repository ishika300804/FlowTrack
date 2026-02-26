package com.example.IMS.controller;

import com.example.IMS.service.EmailService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

/**
 * Razorpay Webhook Receiver
 *
 * Handles inbound events pushed by Razorpay to /payment/webhook.
 * This endpoint is public (no session required) — signature verification
 * ensures only genuine Razorpay calls are processed.
 *
 * Configured events (Razorpay Dashboard → Webhooks → Active Events):
 *   - payment.failed   → PE-04: notify user that payment failed
 *
 * Webhook Secret: set razorpay.webhook.secret in application.properties
 *   (different from razorpay.key.secret — configure in Razorpay Dashboard)
 */
@RestController
@RequestMapping("/payment")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    @Autowired
    private EmailService emailService;

    /**
     * Razorpay sends POST /payment/webhook with:
     *   Header  : X-Razorpay-Signature  (HMAC-SHA256 of body using webhook secret)
     *   Body    : JSON event payload
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        // ── 1. Verify signature ──────────────────────────────────────────────
        if (webhookSecret == null || webhookSecret.isBlank()) {
            logger.error("razorpay.webhook.secret is not configured — rejecting all webhook calls");
            return ResponseEntity.status(500).body("Webhook secret not configured");
        }
        if (signature == null || !isValidSignature(payload, signature)) {
            logger.warn("Razorpay webhook received with invalid or missing signature — rejected");
            return ResponseEntity.status(400).body("Invalid signature");
        }

        // ── 2. Parse event ───────────────────────────────────────────────────
        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.optString("event", "");
            logger.info("Razorpay webhook event received: {}", eventType);

            switch (eventType) {
                case "payment.failed":
                    handlePaymentFailed(event);
                    break;
                default:
                    logger.info("Unhandled Razorpay event type: {} — ignored", eventType);
            }

        } catch (Exception e) {
            logger.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
            // Return 200 to prevent Razorpay from retrying on parse errors
            return ResponseEntity.ok("error logged");
        }

        return ResponseEntity.ok("ok");
    }

    // ── PE-04: payment.failed ─────────────────────────────────────────────────
    private void handlePaymentFailed(JSONObject event) {
        try {
            JSONObject paymentEntity = event
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String paymentId   = paymentEntity.optString("id", "N/A");
            String email       = paymentEntity.optString("email", "");
            String description = paymentEntity.optString("description", "your subscription");
            String errorDesc   = paymentEntity.optString("error_description", "Payment could not be processed");

            logger.info("PE-04 payment.failed — paymentId={}, email={}, desc={}", paymentId, email, errorDesc);

            if (!email.isBlank()) {
                // We don't have the user's name from Razorpay, so use email as fallback
                emailService.sendPaymentFailedEmail(email, email, description);
                logger.info("PE-04 payment-failed email sent to {}", email);
            } else {
                logger.warn("PE-04: payment.failed event has no email — cannot notify user");
            }

        } catch (Exception e) {
            logger.error("PE-04: Error handling payment.failed event — {}", e.getMessage(), e);
        }
    }

    // ── Signature verification ────────────────────────────────────────────────
    private boolean isValidSignature(String payload, String receivedSignature) {
        try {
            Mac sha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256.init(key);
            byte[] hash = sha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            Formatter formatter = new Formatter();
            for (byte b : hash) formatter.format("%02x", b);
            String expected = formatter.toString();
            formatter.close();

            return expected.equals(receivedSignature);
        } catch (Exception e) {
            logger.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
