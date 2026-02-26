package com.example.IMS.service;

import com.example.IMS.dto.EmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.from.email}")
    private String fromEmail;

    @Value("${mail.from.name}")
    private String fromName;

    /**
     * Send a simple plain text email
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send an HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML
            
            mailSender.send(mimeMessage);
            System.out.println("HTML Email sent successfully to: " + to);
        } catch (MessagingException e) {
            System.err.println("Error sending HTML email: " + e.getMessage());
            throw new RuntimeException("Failed to send HTML email", e);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    /**
     * Send email using EmailRequest DTO
     */
    public void sendEmail(EmailRequest emailRequest) {
        if (emailRequest.isHtml()) {
            sendHtmlEmail(emailRequest.getTo(), emailRequest.getSubject(), emailRequest.getBody());
        } else {
            sendSimpleEmail(emailRequest.getTo(), emailRequest.getSubject(), emailRequest.getBody());
        }
    }

    /**
     * Send welcome email to new users
     */
    public void sendWelcomeEmail(String to, String userName, String userType) {
        String subject = "Welcome to FlowTrack - " + userType + " Account Created";
        String body = String.format(
            "<html><body>" +
            "<h2>Welcome to FlowTrack!</h2>" +
            "<p>Dear %s,</p>" +
            "<p>Your %s account has been successfully created.</p>" +
            "<p>You can now log in and start using our inventory management system.</p>" +
            "<br>" +
            "<p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>",
            userName, userType
        );
        
        sendHtmlEmail(to, subject, body);
    }

    /**
     * Send payment confirmation email
     */
    public void sendPaymentConfirmationEmail(String to, String userName, double amount, String transactionId) {
        String subject = "Payment Confirmation - FlowTrack";
        String body = String.format(
            "<html><body>" +
            "<h2>Payment Confirmation</h2>" +
            "<p>Dear %s,</p>" +
            "<p>Your payment has been successfully processed.</p>" +
            "<p><strong>Transaction Details:</strong></p>" +
            "<ul>" +
            "<li>Amount: ₹%.2f</li>" +
            "<li>Transaction ID: %s</li>" +
            "<li>Date: %s</li>" +
            "</ul>" +
            "<p>Thank you for your payment!</p>" +
            "<br>" +
            "<p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>",
            userName, amount, transactionId, new java.util.Date().toString()
        );
        
        sendHtmlEmail(to, subject, body);
    }

    /**
     * Send OTP email for verification
     */
    public void sendOtpEmail(String to, String otp) {
        String subject = "Your FlowTrack OTP Code";
        String body = String.format(
            "<html><body>" +
            "<h2>Email Verification</h2>" +
            "<p>Your OTP code is: <strong style='font-size: 24px; color: #007bff;'>%s</strong></p>" +
            "<p>This OTP is valid for 10 minutes.</p>" +
            "<p>If you didn't request this, please ignore this email.</p>" +
            "<br>" +
            "<p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>",
            otp
        );
        sendHtmlEmail(to, subject, body);
    }

    // ── E-06 ─────────────────────────────────────────────────────────────────
    /** Business profile submitted for verification */
    public void sendProfileSubmittedEmail(String to, String userName) {
        String subject = "FlowTrack — Business Profile Submitted for Verification";
        String body = String.format(
            "<html><body>" +
            "<h2>Profile Submitted ✅</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Your business profile has been submitted for verification.</p>" +
            "<p>Our team will review it and respond within <strong>24–48 hours</strong>. " +
            "You'll receive an email once the review is complete.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", userName);
        sendHtmlEmail(to, subject, body);
    }

    // ── E-07 / E-11 ──────────────────────────────────────────────────────────
    /** Business profile approved — account fully active */
    public void sendProfileApprovedEmail(String to, String userName) {
        String subject = "FlowTrack — Business Profile Approved 🎉";
        String body = String.format(
            "<html><body>" +
            "<h2>Congratulations, %s! Your profile is approved.</h2>" +
            "<p>Your business profile has been <strong>verified and activated</strong>.</p>" +
            "<p>You can now access all inventory, vendor, and reporting features on FlowTrack.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", userName);
        sendHtmlEmail(to, subject, body);
    }

    // ── E-08 ─────────────────────────────────────────────────────────────────
    /** Business profile rejected */
    public void sendProfileRejectedEmail(String to, String userName, String reason) {
        String subject = "FlowTrack — Business Profile Needs Revision";
        String body = String.format(
            "<html><body>" +
            "<h2>Profile Verification — Action Required</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Unfortunately, your business profile could not be verified at this time.</p>" +
            "<p><strong>Reason:</strong> %s</p>" +
            "<p>Please log in, correct the issues, and resubmit your profile for review.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", userName, reason != null ? reason : "Please contact support for details.");
        sendHtmlEmail(to, subject, body);
    }

    // ── E-12 ─────────────────────────────────────────────────────────────────
    /** Item issued to borrower */
    public void sendItemIssuedEmail(String to, String borrowerName, String itemName,
                                    String issueDate, String dueDate) {
        String subject = "FlowTrack — Item Issued: " + itemName;
        String body = String.format(
            "<html><body>" +
            "<h2>Item Issued 📦</h2>" +
            "<p>Hi %s,</p>" +
            "<p>The following item has been issued to you:</p>" +
            "<ul><li><strong>Item:</strong> %s</li>" +
            "<li><strong>Issue Date:</strong> %s</li>" +
            "<li><strong>Due Date:</strong> %s</li></ul>" +
            "<p>Please ensure the item is returned by the due date to avoid fines.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", borrowerName, itemName, issueDate, dueDate);
        sendHtmlEmail(to, subject, body);
    }

    // ── E-13 ─────────────────────────────────────────────────────────────────
    /** Due date reminder (scheduled) */
    public void sendDueDateReminderEmail(String to, String borrowerName,
                                          String itemName, String dueDate, long daysLeft) {
        String subject = "FlowTrack — Reminder: Return \"" + itemName + "\" in " + daysLeft + " day(s)";
        String body = String.format(
            "<html><body>" +
            "<h2>⏰ Return Reminder</h2>" +
            "<p>Hi %s,</p>" +
            "<p>This is a reminder that <strong>%s</strong> is due for return in <strong>%d day(s)</strong>.</p>" +
            "<p><strong>Due Date:</strong> %s</p>" +
            "<p>Please return the item on time to avoid late fines.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", borrowerName, itemName, daysLeft, dueDate);
        sendHtmlEmail(to, subject, body);
    }

    // ── E-14 ─────────────────────────────────────────────────────────────────
    /** Overdue notice (scheduled) */
    public void sendOverdueNoticeEmail(String to, String borrowerName,
                                        String itemName, String dueDate, long daysOverdue) {
        String subject = "FlowTrack — OVERDUE: \"" + itemName + "\" (" + daysOverdue + " day(s) late)";
        String body = String.format(
            "<html><body>" +
            "<h2>⚠️ Item Overdue</h2>" +
            "<p>Hi %s,</p>" +
            "<p><strong>%s</strong> was due on <strong>%s</strong> and is now <strong>%d day(s) overdue</strong>.</p>" +
            "<p>A fine is accumulating. Please return the item immediately to stop further charges.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", borrowerName, itemName, dueDate, daysOverdue);
        sendHtmlEmail(to, subject, body);
    }

    // ── E-16 ─────────────────────────────────────────────────────────────────
    /** Item returned */
    public void sendItemReturnedEmail(String to, String borrowerName,
                                       String itemName, double totalFine) {
        String fineMsg = totalFine > 0
            ? String.format("A fine of <strong>₹%.2f</strong> is outstanding on your account.", totalFine)
            : "No outstanding fines — your account is clear.";
        String subject = "FlowTrack — Item Returned: " + itemName;
        String body = String.format(
            "<html><body>" +
            "<h2>Item Return Acknowledged ✅</h2>" +
            "<p>Hi %s,</p>" +
            "<p>We have received <strong>%s</strong>. Thank you for returning it.</p>" +
            "<p>%s</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", borrowerName, itemName, fineMsg);
        sendHtmlEmail(to, subject, body);
    }

    // ── E-17 ─────────────────────────────────────────────────────────────────
    /** Repair request sent to vendor */
    public void sendRepairRequestEmail(String to, String vendorName,
                                        String itemName, double estimatedCost) {
        String costStr = estimatedCost > 0
            ? String.format("Estimated cost: <strong>₹%.2f</strong>", estimatedCost)
            : "Estimated cost: <strong>To be confirmed</strong>";
        String subject = "FlowTrack — Repair Request: " + itemName;
        String body = String.format(
            "<html><body>" +
            "<h2>🔧 New Repair Request</h2>" +
            "<p>Hi %s,</p>" +
            "<p>A repair request has been raised for <strong>%s</strong>.</p>" +
            "<p>%s</p>" +
            "<p>Please confirm receipt and proceed with the repair at your earliest convenience.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", vendorName, itemName, costStr);
        sendHtmlEmail(to, subject, body);
    }

    // ── E-18 ─────────────────────────────────────────────────────────────────
    /** Repair completed — notify retailer */
    public void sendRepairCompletedEmail(String to, String userName, String itemName) {
        String subject = "FlowTrack — Repair Completed: " + itemName;
        String body = String.format(
            "<html><body>" +
            "<h2>✅ Repair Completed</h2>" +
            "<p>Hi %s,</p>" +
            "<p>The repair for <strong>%s</strong> has been completed and the item is ready.</p>" +
            "<p>Please arrange to collect or confirm dispatch.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", userName, itemName);
        sendHtmlEmail(to, subject, body);
    }

    // ── E-19 ─────────────────────────────────────────────────────────────────
    /** Welcome email for newly added vendor */
    public void sendVendorWelcomeEmail(String to, String vendorName) {
        String subject = "Welcome to FlowTrack — Vendor Portal";
        String body = String.format(
            "<html><body>" +
            "<h2>Welcome, %s! 🎉</h2>" +
            "<p>You have been registered as a vendor on <strong>FlowTrack</strong>.</p>" +
            "<p>You'll receive repair requests and procurement orders through this platform.</p>" +
            "<p>For queries, contact your FlowTrack account manager.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", vendorName);
        sendHtmlEmail(to, subject, body);
    }

    // ── PE-01/02/03 ──────────────────────────────────────────────────────────
    /** Subscription activated after successful payment */
    public void sendSubscriptionActivatedEmail(String to, String userName,
                                                String planName, String validUntil,
                                                String transactionId) {
        String subject = "FlowTrack — " + planName + " Subscription Activated";
        String body = String.format(
            "<html><body>" +
            "<h2>🎉 Subscription Activated!</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Your <strong>%s</strong> subscription is now active.</p>" +
            "<ul><li><strong>Valid Until:</strong> %s</li>" +
            "<li><strong>Transaction ID:</strong> %s</li></ul>" +
            "<p>Enjoy unlimited access to all features included in your plan.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", userName, planName, validUntil, transactionId);
        sendHtmlEmail(to, subject, body);
    }

    // ── PE-04 ─────────────────────────────────────────────────────────────────
    /** Payment failed notification */
    public void sendPaymentFailedEmail(String to, String userName, String context) {
        String subject = "FlowTrack — Payment Failed";
        String body = String.format(
            "<html><body>" +
            "<h2>⚠️ Payment Failed</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Your payment for <strong>%s</strong> could not be processed.</p>" +
            "<p>Please retry your payment or update your payment method to avoid service interruption.</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", userName, context);
        sendHtmlEmail(to, subject, body);
    }

    // ── PE-05 ─────────────────────────────────────────────────────────────────
    /** Fine payment receipt */
    public void sendFinePaymentReceiptEmail(String to, String borrowerName,
                                             double amountPaid, double remainingFine) {
        String remaining = remainingFine > 0
            ? String.format("Remaining balance: <strong>₹%.2f</strong>", remainingFine)
            : "Your fine account is now <strong>fully cleared</strong>.";
        String subject = "FlowTrack — Fine Payment Receipt";
        String body = String.format(
            "<html><body>" +
            "<h2>Fine Payment Received ✅</h2>" +
            "<p>Hi %s,</p>" +
            "<p>We have received your fine payment of <strong>₹%.2f</strong>.</p>" +
            "<p>%s</p>" +
            "<br><p>Best regards,<br>FlowTrack Team</p>" +
            "</body></html>", borrowerName, amountPaid, remaining);
        sendHtmlEmail(to, subject, body);
    }
}
