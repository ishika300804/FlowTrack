package com.example.IMS.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class VendorRegistrationDto {
    
    // User Basic Info
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    // Vendor-Specific Fields
    @NotBlank(message = "Company name is required")
    private String companyName;
    
    @NotBlank(message = "Business type is required")
    private String businessType;
    
    private String tradeLicenseNumber;
    
    private String gstNumber;
    
    private String panNumber;
    
    @NotBlank(message = "Company address is required")
    private String companyAddress;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    private String productCategories;
    
    private String companyDescription;
    
    // Bank Details
    private String bankAccountNumber;
    private String bankName;
    private String bankIfscCode;
    
    // File uploads (will handle as multipart files in controller)
    private String tradeLicenseUrl;
    private String gstCertificateUrl;
    private String companyRegistrationUrl;
    
    // Constructors
    public VendorRegistrationDto() {}
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
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
}
