package com.example.IMS.dto;

import com.example.IMS.model.enums.BankVerificationStatus;

import java.time.LocalDateTime;

/**
 * DTO for BankDetails API responses
 * SECURITY: Account numbers are ALWAYS masked in responses
 * Only authorized users with FINANCE role or higher can view masked account numbers
 */
public class BankDetailsDTO {
    
    private Long id;
    private Long businessProfileId;
    private String accountHolderName;
    private String maskedAccountNumber;  // e.g., "****1234"
    private String ifscCode;
    private String bankName;
    private String branchName;
    private Boolean isPrimary;
    private BankVerificationStatus bankVerificationStatus;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // For UI display
    private Boolean canUseForSettlement;
    private String statusDisplayColor;  // "success", "warning", "danger"
    
    // Constructors
    public BankDetailsDTO() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getBusinessProfileId() {
        return businessProfileId;
    }
    
    public void setBusinessProfileId(Long businessProfileId) {
        this.businessProfileId = businessProfileId;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }
    
    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
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
    
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    public BankVerificationStatus getBankVerificationStatus() {
        return bankVerificationStatus;
    }
    
    public void setBankVerificationStatus(BankVerificationStatus bankVerificationStatus) {
        this.bankVerificationStatus = bankVerificationStatus;
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
    
    public Boolean getCanUseForSettlement() {
        return canUseForSettlement;
    }
    
    public void setCanUseForSettlement(Boolean canUseForSettlement) {
        this.canUseForSettlement = canUseForSettlement;
    }
    
    public String getStatusDisplayColor() {
        return statusDisplayColor;
    }
    
    public void setStatusDisplayColor(String statusDisplayColor) {
        this.statusDisplayColor = statusDisplayColor;
    }
}
