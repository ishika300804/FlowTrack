package com.example.IMS.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Table(name = "vendor_profiles")
public class VendorProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @NotBlank(message = "Company name is required")
    @Column(name = "company_name", nullable = false)
    private String companyName;
    
    @NotBlank(message = "Business type is required")
    @Column(name = "business_type", nullable = false)
    private String businessType; // Manufacturer, Wholesaler, Distributor, etc.
    
    @Column(name = "trade_license_number")
    private String tradeLicenseNumber;
    
    @Column(name = "gst_number")
    private String gstNumber;
    
    @Column(name = "pan_number")
    private String panNumber;
    
    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "product_categories", columnDefinition = "TEXT")
    private String productCategories; // Comma-separated or JSON
    
    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;
    
    @Column(name = "bank_account_number")
    private String bankAccountNumber;
    
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "bank_ifsc_code")
    private String bankIfscCode;
    
    @Column(name = "trade_license_url")
    private String tradeLicenseUrl; // Document upload path
    
    @Column(name = "gst_certificate_url")
    private String gstCertificateUrl; // Document upload path
    
    @Column(name = "company_registration_url")
    private String companyRegistrationUrl; // Document upload path
    
    @Column(name = "verification_status")
    private String verificationStatus = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    // Constructors
    public VendorProfile() {}
    
    public VendorProfile(User user, String companyName, String businessType) {
        this.user = user;
        this.companyName = companyName;
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
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getBusinessType() {
        return businessType;
    }
    
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
    
    public String getTradeLicenseNumber() {
        return tradeLicenseNumber;
    }
    
    public void setTradeLicenseNumber(String tradeLicenseNumber) {
        this.tradeLicenseNumber = tradeLicenseNumber;
    }
    
    public String getGstNumber() {
        return gstNumber;
    }
    
    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }
    
    public String getPanNumber() {
        return panNumber;
    }
    
    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }
    
    public String getCompanyAddress() {
        return companyAddress;
    }
    
    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getProductCategories() {
        return productCategories;
    }
    
    public void setProductCategories(String productCategories) {
        this.productCategories = productCategories;
    }
    
    public String getCompanyDescription() {
        return companyDescription;
    }
    
    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }
    
    public String getBankAccountNumber() {
        return bankAccountNumber;
    }
    
    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public String getBankIfscCode() {
        return bankIfscCode;
    }
    
    public void setBankIfscCode(String bankIfscCode) {
        this.bankIfscCode = bankIfscCode;
    }
    
    public String getTradeLicenseUrl() {
        return tradeLicenseUrl;
    }
    
    public void setTradeLicenseUrl(String tradeLicenseUrl) {
        this.tradeLicenseUrl = tradeLicenseUrl;
    }
    
    public String getGstCertificateUrl() {
        return gstCertificateUrl;
    }
    
    public void setGstCertificateUrl(String gstCertificateUrl) {
        this.gstCertificateUrl = gstCertificateUrl;
    }
    
    public String getCompanyRegistrationUrl() {
        return companyRegistrationUrl;
    }
    
    public void setCompanyRegistrationUrl(String companyRegistrationUrl) {
        this.companyRegistrationUrl = companyRegistrationUrl;
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
