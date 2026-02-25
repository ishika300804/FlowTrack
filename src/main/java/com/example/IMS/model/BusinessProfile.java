package com.example.IMS.model;

import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.OnboardingStage;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.util.SensitiveDataEncryptionConverter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business Profile Entity
 * Represents a business entity with legal and compliance information
 * Supports multi-tenancy: One user can manage multiple businesses
 * 
 * IMPORTANT: All business operations must be scoped by business_profile_id
 * to ensure tenant isolation and prevent cross-profile data leakage.
 */
@Entity
@Table(name = "business_profiles")
public class BusinessProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Many-to-One relationship with User
     * One user can manage multiple business profiles
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
    
    @NotBlank(message = "Legal business name is required")
    @Column(name = "legal_business_name", nullable = false)
    private String legalBusinessName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 20)
    @NotNull(message = "Business type is required")
    private BusinessType businessType;
    
    /**
     * GSTIN (Goods and Services Tax Identification Number)
     * Format: 15 characters (e.g., 29ABCDE1234F1Z5)
     * Unique across all business profiles
     */
    @NotBlank(message = "GSTIN is required")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", 
             message = "Invalid GSTIN format")
    @Column(name = "gstin", unique = true, nullable = false, length = 15)
    private String gstin;
    
    /**
     * PAN Number (Permanent Account Number)
     * Encrypted at rest for security
     * Format: 10 characters (e.g., ABCDE1234F)
     */
    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", 
             message = "Invalid PAN format")
    @Convert(converter = SensitiveDataEncryptionConverter.class)
    @Column(name = "pan_number", nullable = false, length = 500)
    private String panNumber;
    
    /**
     * CIN (Corporate Identification Number) - for companies
     * Format: 21 characters (e.g., U12345MH2020PTC123456)
     * Optional - only for Private Limited and LLP
     */
    @Pattern(regexp = "^[UL][0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$", 
             message = "Invalid CIN format")
    @Column(name = "cin_number", length = 21)
    private String cinNumber;
    
    /**
     * Udyam Registration Number - for MSMEs
     * Format: UDYAM-XX-00-0000000 (19 characters)
     * Optional
     */
    @Pattern(regexp = "^UDYAM-[A-Z]{2}-[0-9]{2}-[0-9]{7}$", 
             message = "Invalid Udyam number format")
    @Column(name = "udyam_number", length = 19)
    private String udyamNumber;
    
    @NotBlank(message = "Registered address is required")
    @Column(name = "registered_address", columnDefinition = "TEXT", nullable = false)
    private String registeredAddress;
    
    @NotBlank(message = "State is required")
    @Column(name = "state", nullable = false, length = 100)
    private String state;
    
    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pincode format")
    @Column(name = "pincode", nullable = false, length = 10)
    private String pincode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @NotNull
    private VerificationStatus verificationStatus = VerificationStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_stage", length = 20)
    private OnboardingStage onboardingStage;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * One-to-Many relationship with BankDetails
     * One business can have multiple bank accounts
     */
    @OneToMany(mappedBy = "businessProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BankDetails> bankAccounts = new ArrayList<>();
    
    /**
     * One-to-Many relationship with VerificationLog
     * Audit trail of all verification activities
     */
    @OneToMany(mappedBy = "businessProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VerificationLog> verificationLogs = new ArrayList<>();
    
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
    public BusinessProfile() {}
    
    public BusinessProfile(User user, String legalBusinessName, BusinessType businessType, 
                          String gstin, String panNumber) {
        this.user = user;
        this.legalBusinessName = legalBusinessName;
        this.businessType = businessType;
        this.gstin = gstin;
        this.panNumber = panNumber;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getLegalBusinessName() {
        return legalBusinessName;
    }
    
    public void setLegalBusinessName(String legalBusinessName) {
        this.legalBusinessName = legalBusinessName;
    }
    
    public BusinessType getBusinessType() {
        return businessType;
    }
    
    public void setBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }
    
    public String getGstin() {
        return gstin;
    }
    
    public void setGstin(String gstin) {
        this.gstin = gstin;
    }
    
    public String getPanNumber() {
        return panNumber;
    }
    
    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }
    
    public String getCinNumber() {
        return cinNumber;
    }
    
    public void setCinNumber(String cinNumber) {
        this.cinNumber = cinNumber;
    }
    
    public String getUdyamNumber() {
        return udyamNumber;
    }
    
    public void setUdyamNumber(String udyamNumber) {
        this.udyamNumber = udyamNumber;
    }
    
    public String getRegisteredAddress() {
        return registeredAddress;
    }
    
    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getPincode() {
        return pincode;
    }
    
    public void setPincode(String pincode) {
        this.pincode = pincode;
    }
    
    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }
    
    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
    
    public OnboardingStage getOnboardingStage() {
        return onboardingStage;
    }
    
    public void setOnboardingStage(OnboardingStage onboardingStage) {
        this.onboardingStage = onboardingStage;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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
    
    public List<BankDetails> getBankAccounts() {
        return bankAccounts;
    }
    
    public void setBankAccounts(List<BankDetails> bankAccounts) {
        this.bankAccounts = bankAccounts;
    }
    
    public List<VerificationLog> getVerificationLogs() {
        return verificationLogs;
    }
    
    public void setVerificationLogs(List<VerificationLog> verificationLogs) {
        this.verificationLogs = verificationLogs;
    }
    
    // Helper methods
    
    /**
     * Add a bank account to this business profile
     */
    public void addBankAccount(BankDetails bankDetails) {
        bankAccounts.add(bankDetails);
        bankDetails.setBusinessProfile(this);
    }
    
    /**
     * Remove a bank account from this business profile
     */
    public void removeBankAccount(BankDetails bankDetails) {
        bankAccounts.remove(bankDetails);
        bankDetails.setBusinessProfile(null);
    }
    
    /**
     * Add a verification log entry
     */
    public void addVerificationLog(VerificationLog log) {
        verificationLogs.add(log);
        log.setBusinessProfile(this);
    }
    
    /**
     * Check if business can perform transactions
     */
    public boolean canTransact() {
        return verificationStatus == VerificationStatus.VERIFIED 
            && onboardingStage == OnboardingStage.ACTIVE;
    }
    
    /**
     * Check if profile is editable
     */
    public boolean isEditable() {
        return verificationStatus == VerificationStatus.DRAFT 
            || verificationStatus == VerificationStatus.REJECTED;
    }
}
