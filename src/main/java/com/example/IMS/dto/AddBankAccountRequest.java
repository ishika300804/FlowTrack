package com.example.IMS.dto;

import javax.validation.constraints.*;

/**
 * DTO for adding a new bank account to a business profile
 * Used in API requests
 */
public class AddBankAccountRequest {
    
    @NotBlank(message = "Account holder name is required")
    @Size(max = 255, message = "Account holder name cannot exceed 255 characters")
    private String accountHolderName;
    
    @NotBlank(message = "Account number is required")
    @Pattern(
        regexp = "^[0-9]{9,18}$",
        message = "Invalid account number. Must be 9-18 digits"
    )
    private String accountNumber;
    
    @NotBlank(message = "IFSC code is required")
    @Pattern(
        regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
        message = "Invalid IFSC code. Format: SBIN0001234"
    )
    private String ifscCode;
    
    @NotBlank(message = "Bank name is required")
    @Size(max = 255, message = "Bank name cannot exceed 255 characters")
    private String bankName;
    
    @Size(max = 255, message = "Branch name cannot exceed 255 characters")
    private String branchName;
    
    @NotNull(message = "Primary flag is required")
    private Boolean isPrimary;
    
    // Constructors
    public AddBankAccountRequest() {}
    
    // Getters and Setters
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
    
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
