package com.example.IMS.model;

import com.example.IMS.model.enums.VerificationType;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Verification Log Entity
 * Maintains complete audit trail of all verification activities
 * 
 * PURPOSE:
 * - Regulatory compliance and audit trail
 * - Debugging verification failures
 * - Fraud detection and analysis
 * - Performance monitoring of verification providers
 * 
 * SECURITY:
 * - Never log sensitive PII in plaintext
 * - Mask sensitive fields in request/response payloads
 * - Include IP address and user agent for security audit
 */
@Entity
@Table(name = "verification_logs")
public class VerificationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Many-to-One relationship with BusinessProfile
     * One business can have multiple verification attempts
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_profile_id", nullable = false)
    @NotNull(message = "Business profile is required")
    private BusinessProfile businessProfile;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 20)
    @NotNull(message = "Verification type is required")
    private VerificationType verificationType;
    
    /**
     * API request payload (JSON format)
     * Store sanitized request for debugging
     * WARNING: Mask sensitive fields before storing
     */
    @Column(name = "request_payload", columnDefinition = "json")
    private String requestPayload;
    
    /**
     * API response payload (JSON format)
     * Store complete response for audit trail
     * WARNING: Mask sensitive fields before storing
     */
    @Column(name = "response_payload", columnDefinition = "json")
    private String responsePayload;
    
    @NotBlank(message = "Verification result is required")
    @Column(name = "verification_result", nullable = false, length = 50)
    private String verificationResult; // SUCCESS, FAILED, ERROR, TIMEOUT, etc.
    
    /**
     * Verification provider name
     * e.g., "Razorpay", "Cashfree", "GSTN API", "NSDL PAN Verification"
     */
    @Column(name = "verification_provider", length = 100)
    private String verificationProvider;
    
    /**
     * External reference ID from verification provider
     * Used for support and reconciliation
     */
    @Column(name = "external_reference_id", length = 100)
    private String externalReferenceId;
    
    /**
     * Error message if verification failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * IP address of the request initiator
     * For security audit and fraud detection
     */
    @Column(name = "ip_address", length = 45) // IPv6 max length
    private String ipAddress;
    
    /**
     * User agent of the request initiator
     * For security audit
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Lifecycle callback
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public VerificationLog() {}
    
    public VerificationLog(BusinessProfile businessProfile, VerificationType verificationType, 
                          String verificationResult) {
        this.businessProfile = businessProfile;
        this.verificationType = verificationType;
        this.verificationResult = verificationResult;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BusinessProfile getBusinessProfile() {
        return businessProfile;
    }
    
    public void setBusinessProfile(BusinessProfile businessProfile) {
        this.businessProfile = businessProfile;
    }
    
    public VerificationType getVerificationType() {
        return verificationType;
    }
    
    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
    }
    
    public String getRequestPayload() {
        return requestPayload;
    }
    
    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }
    
    public String getResponsePayload() {
        return responsePayload;
    }
    
    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }
    
    public String getVerificationResult() {
        return verificationResult;
    }
    
    public void setVerificationResult(String verificationResult) {
        this.verificationResult = verificationResult;
    }
    
    public String getVerificationProvider() {
        return verificationProvider;
    }
    
    public void setVerificationProvider(String verificationProvider) {
        this.verificationProvider = verificationProvider;
    }
    
    public String getExternalReferenceId() {
        return externalReferenceId;
    }
    
    public void setExternalReferenceId(String externalReferenceId) {
        this.externalReferenceId = externalReferenceId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Helper methods
    
    /**
     * Check if verification was successful
     */
    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(verificationResult);
    }
    
    /**
     * Check if verification failed
     */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(verificationResult);
    }
    
    /**
     * Check if verification had an error
     */
    public boolean hasError() {
        return "ERROR".equalsIgnoreCase(verificationResult) || 
               "TIMEOUT".equalsIgnoreCase(verificationResult);
    }
}
