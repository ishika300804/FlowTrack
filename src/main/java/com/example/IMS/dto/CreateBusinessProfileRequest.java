package com.example.IMS.dto;

import com.example.IMS.model.enums.BusinessType;

import javax.validation.constraints.*;

/**
 * DTO for creating a new business profile
 * Used in API requests and form submissions
 * 
 * Security: Does NOT include encrypted fields in plaintext form
 * Validation: All fields validated before entity creation
 */
public class CreateBusinessProfileRequest {
    
    @NotBlank(message = "Legal business name is required")
    @Size(min = 3, max = 255, message = "Business name must be between 3 and 255 characters")
    private String legalBusinessName;
    
    @NotNull(message = "Business type is required")
    private BusinessType businessType;
    
    @NotBlank(message = "GSTIN is required")
    @Pattern(
        regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
        message = "Invalid GSTIN format. Format: 29ABCDE1234F1Z5"
    )
    private String gstin;
    
    @NotBlank(message = "PAN number is required")
    @Pattern(
        regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
        message = "Invalid PAN format. Format: ABCDE1234F"
    )
    private String panNumber;
    
    // Optional fields
    @Pattern(
        regexp = "^[LUF][0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$",
        message = "Invalid CIN format. Format: L12345MH2024PLC123456"
    )
    private String cinNumber;
    
    @Pattern(
        regexp = "^UDYAM-[A-Z]{2}-[0-9]{2}-[0-9]{7}$",
        message = "Invalid Udyam number format. Format: UDYAM-KA-26-1234567"
    )
    private String udyamNumber;
    
    @NotBlank(message = "Registered address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String registeredAddress;
    
    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State name cannot exceed 100 characters")
    private String state;
    
    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pincode. Must be 6 digits")
    private String pincode;
    
    // Contact information
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String contactEmail;
    
    @Pattern(
        regexp = "^[6-9][0-9]{9}$",
        message = "Invalid phone number. Must be 10 digits starting with 6-9"
    )
    private String contactPhone;
    
    // Optional: Director/Partner details (for verification)
    @Size(max = 255, message = "Director name cannot exceed 255 characters")
    private String directorName;
    
    @Pattern(
        regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
        message = "Invalid director PAN format"
    )
    private String directorPan;
    
    // Constructors
    public CreateBusinessProfileRequest() {}
    
    // Getters and Setters
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
    
    public String getDirectorPan() {
        return directorPan;
    }
    
    public void setDirectorPan(String directorPan) {
        this.directorPan = directorPan;
    }
}
