package com.example.IMS.model;

import com.example.IMS.model.enums.BankVerificationStatus;
import com.example.IMS.util.SensitiveDataEncryptionConverter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * Bank Details Entity
 * Stores bank account information for business profiles
 * Supports multiple bank accounts per business with one primary account
 * 
 * IMPORTANT CONSTRAINTS:
 * - Only one account can be marked as primary per business
 * - Account numbers are encrypted at rest
 * - Primary verified account must be used for settlements
 * - Cannot delete primary account unless another is set as primary
 */
@Entity
@Table(name = "bank_details")
public class BankDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Many-to-One relationship with BusinessProfile
     * One business can have multiple bank accounts
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_profile_id", nullable = false)
    @NotNull(message = "Business profile is required")
    private BusinessProfile businessProfile;
    
    @NotBlank(message = "Account holder name is required")
    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;
    
    /**
     * Bank account number - encrypted at rest
     * Format varies by bank (typically 9-18 digits)
     */
    @NotBlank(message = "Account number is required")
    @Convert(converter = SensitiveDataEncryptionConverter.class)
    @Column(name = "account_number", nullable = false, length = 500)
    private String accountNumber;
    
    /**
     * IFSC Code (Indian Financial System Code)
     * Format: 11 characters (e.g., SBIN0001234)
     */
    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", 
             message = "Invalid IFSC code format")
    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;
    
    @NotBlank(message = "Bank name is required")
    @Column(name = "bank_name", nullable = false)
    private String bankName;
    
    @Column(name = "branch_name")
    private String branchName;
    
    /**
     * Primary account flag
     * Only one account can be primary per business profile
     * Enforced by database trigger
     */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bank_verification_status", nullable = false, length = 20)
    @NotNull
    private BankVerificationStatus bankVerificationStatus = BankVerificationStatus.UNVERIFIED;
    
    /**
     * Reference ID from verification provider (e.g., Razorpay, Cashfree)
     */
    @Column(name = "verification_reference_id", length = 100)
    private String verificationReferenceId;
    
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public BankDetails() {}
    
    public BankDetails(BusinessProfile businessProfile, String accountHolderName, 
                      String accountNumber, String ifscCode, String bankName) {
        this.businessProfile = businessProfile;
        this.accountHolderName = accountHolderName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.bankName = bankName;
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
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getIfscCode() {
        return ifscCode;
    }
    
    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public String getBranchName() {
        return branchName;
    }
    
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
    
    public boolean isPrimary() {
        return isPrimary;
    }
    
    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
    
    public BankVerificationStatus getBankVerificationStatus() {
        return bankVerificationStatus;
    }
    
    public void setBankVerificationStatus(BankVerificationStatus bankVerificationStatus) {
        this.bankVerificationStatus = bankVerificationStatus;
    }
    
    public String getVerificationReferenceId() {
        return verificationReferenceId;
    }
    
    public void setVerificationReferenceId(String verificationReferenceId) {
        this.verificationReferenceId = verificationReferenceId;
    }
    
    public LocalDateTime getLastVerifiedAt() {
        return lastVerifiedAt;
    }
    
    public void setLastVerifiedAt(LocalDateTime lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Helper methods
    
    /**
     * Check if this account can be used for settlements
     */
    public boolean canUseForSettlement() {
        return isPrimary && bankVerificationStatus == BankVerificationStatus.VERIFIED;
    }
    
    /**
     * Mark verification as successful
     */
    public void markAsVerified(String referenceId) {
        this.bankVerificationStatus = BankVerificationStatus.VERIFIED;
        this.verificationReferenceId = referenceId;
        this.lastVerifiedAt = LocalDateTime.now();
    }
    
    /**
     * Mark verification as failed
     */
    public void markVerificationFailed() {
        this.bankVerificationStatus = BankVerificationStatus.FAILED;
        this.lastVerifiedAt = LocalDateTime.now();
    }
    
    /**
     * Get masked account number for display (e.g., XXXX1234)
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "XXXX";
        }
        int length = accountNumber.length();
        return "X".repeat(length - 4) + accountNumber.substring(length - 4);
    }
}
