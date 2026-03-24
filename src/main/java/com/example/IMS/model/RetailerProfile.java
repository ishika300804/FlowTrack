package com.example.IMS.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Table(name = "retailer_profiles")
public class RetailerProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @NotBlank(message = "Business name is required")
    @Column(name = "business_name", nullable = false)
    private String businessName;
    
    @NotBlank(message = "Business type is required")
    @Column(name = "business_type", nullable = false)
    private String businessType; // Retail Store, E-commerce, Restaurant, etc.
    
    @Column(name = "trademark")
    private String trademark;
    
    @Column(name = "business_registration_number")
    private String businessRegistrationNumber;
    
    @Column(name = "gst_number")
    private String gstNumber;
    
    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "business_description", columnDefinition = "TEXT")
    private String businessDescription;
    
    @Column(name = "proof_of_identity_url")
    private String proofOfIdentityUrl; // Document upload path
    
    @Column(name = "business_license_url")
    private String businessLicenseUrl; // Document upload path
    
    @Column(name = "verification_status")
    private String verificationStatus = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    // Constructors
    public RetailerProfile() {}
    
    public RetailerProfile(User user, String businessName, String businessType) {
        this.user = user;
        this.businessName = businessName;
        this.businessType = businessType;
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
    
    public String getBusinessName() {
        return businessName;
    }
    
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
    
    public String getBusinessType() {
        return businessType;
    }
    
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
    
    public String getTrademark() {
        return trademark;
    }
    
    public void setTrademark(String trademark) {
        this.trademark = trademark;
    }
    
    public String getBusinessRegistrationNumber() {
        return businessRegistrationNumber;
    }
    
    public void setBusinessRegistrationNumber(String businessRegistrationNumber) {
        this.businessRegistrationNumber = businessRegistrationNumber;
    }
    
    public String getGstNumber() {
        return gstNumber;
    }
    
    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }
    
    public String getBusinessAddress() {
        return businessAddress;
    }
    
    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getBusinessDescription() {
        return businessDescription;
    }
    
    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }
    
    public String getProofOfIdentityUrl() {
        return proofOfIdentityUrl;
    }
    
    public void setProofOfIdentityUrl(String proofOfIdentityUrl) {
        this.proofOfIdentityUrl = proofOfIdentityUrl;
    }
    
    public String getBusinessLicenseUrl() {
        return businessLicenseUrl;
    }
    
    public void setBusinessLicenseUrl(String businessLicenseUrl) {
        this.businessLicenseUrl = businessLicenseUrl;
    }
    
    public String getVerificationStatus() {
        return verificationStatus;
    }
    
    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
