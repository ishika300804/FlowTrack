package com.example.IMS.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RetailerRegistrationDto {
    
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
    
    // Retailer-Specific Fields
    @NotBlank(message = "Business name is required")
    private String businessName;
    
    @NotBlank(message = "Business type is required")
    private String businessType;
    
    private String trademark;
    
    private String businessRegistrationNumber;
    
    private String gstNumber;
    
    @NotBlank(message = "Business address is required")
    private String businessAddress;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    private String businessDescription;
    
    // File uploads (will handle as multipart files in controller)
    private String proofOfIdentityUrl;
    private String businessLicenseUrl;
    
    // Constructors
    public RetailerRegistrationDto() {}
    
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
}
