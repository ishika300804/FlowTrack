package com.example.IMS.dto;

import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.OnboardingStage;
import com.example.IMS.model.enums.VerificationStatus;

import java.time.LocalDateTime;

/**
 * DTO for BusinessProfile API responses
 * SECURITY: Sensitive data is masked or excluded
 * - PAN numbers are masked (ABCDE****F)
 * - Account numbers are masked (shown in BankDetailsDTO)
 * - Full details only shown to authorized users
 */
public class BusinessProfileDTO {
    
    private Long id;
    private String legalBusinessName;
    private BusinessType businessType;
    private String gstin;
    private String maskedPanNumber;  // ABCDE****F
    private String cinNumber;
    private String udyamNumber;
    private String registeredAddress;
    private String state;
    private String pincode;
    private String contactEmail;
    private String contactPhone;
    private String directorName;
    private VerificationStatus verificationStatus;
    private OnboardingStage onboardingStage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User information (minimal)
    private Long userId;
    private String userName;
    
    // Statistics
    private Integer totalBankAccounts;
    private Integer verifiedBankAccounts;
    private Boolean hasActivePrimaryAccount;
    
    // Permission information (for current user)
    private String userRole;  // Their role in this business
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canManageTeam;
    
    // Constructors
    public BusinessProfileDTO() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getMaskedPanNumber() {
        return maskedPanNumber;
    }
    
    public void setMaskedPanNumber(String maskedPanNumber) {
        this.maskedPanNumber = maskedPanNumber;
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
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    public String getDirectorName() {
        return directorName;
    }
    
    public void setDirectorName(String directorName) {
        this.directorName = directorName;
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
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public Integer getTotalBankAccounts() {
        return totalBankAccounts;
    }
    
    public void setTotalBankAccounts(Integer totalBankAccounts) {
        this.totalBankAccounts = totalBankAccounts;
    }
    
    public Integer getVerifiedBankAccounts() {
        return verifiedBankAccounts;
    }
    
    public void setVerifiedBankAccounts(Integer verifiedBankAccounts) {
        this.verifiedBankAccounts = verifiedBankAccounts;
    }
    
    public Boolean getHasActivePrimaryAccount() {
        return hasActivePrimaryAccount;
    }
    
    public void setHasActivePrimaryAccount(Boolean hasActivePrimaryAccount) {
        this.hasActivePrimaryAccount = hasActivePrimaryAccount;
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
    
    public Boolean getCanEdit() {
        return canEdit;
    }
    
    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }
    
    public Boolean getCanDelete() {
        return canDelete;
    }
    
    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }
    
    public Boolean getCanManageTeam() {
        return canManageTeam;
    }
    
    public void setCanManageTeam(Boolean canManageTeam) {
        this.canManageTeam = canManageTeam;
    }
}
