package com.example.IMS.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class InvestorRegistrationDto {
    
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
    
    // Investor-Specific Fields
    @NotBlank(message = "Investor name is required")
    private String investorName;
    
    @NotBlank(message = "Investor type is required")
    private String investorType;
    
    private String panNumber;
    
    private String aadharNumber;
    
    private String companyRegistrationNumber;
    
    private BigDecimal investmentCapacity;
    
    private BigDecimal minimumInvestmentAmount;
    
    private BigDecimal maximumInvestmentAmount;
    
    private String preferredSectors;
    
    private Integer investmentExperienceYears;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    // Bank Details
    private String bankAccountNumber;
    private String bankName;
    private String bankIfscCode;
    
    // Investment Preferences
    private String riskAppetite; // LOW, MEDIUM, HIGH
    private BigDecimal expectedRoiPercentage;
    
    // File uploads (will handle as multipart files in controller)
    private String panCardUrl;
    private String aadharCardUrl;
    private String bankStatementUrl;
    private String investmentPortfolioUrl;
    
    // Constructors
    public InvestorRegistrationDto() {}
    
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
    
    public String getInvestorName() {
        return investorName;
    }
    
    public void setInvestorName(String investorName) {
        this.investorName = investorName;
    }
    
    public String getInvestorType() {
        return investorType;
    }
    
    public void setInvestorType(String investorType) {
        this.investorType = investorType;
    }
    
    public String getPanNumber() {
        return panNumber;
    }
    
    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }
    
    public String getAadharNumber() {
        return aadharNumber;
    }
    
    public void setAadharNumber(String aadharNumber) {
        this.aadharNumber = aadharNumber;
    }
    
    public String getCompanyRegistrationNumber() {
        return companyRegistrationNumber;
    }
    
    public void setCompanyRegistrationNumber(String companyRegistrationNumber) {
        this.companyRegistrationNumber = companyRegistrationNumber;
    }
    
    public BigDecimal getInvestmentCapacity() {
        return investmentCapacity;
    }
    
    public void setInvestmentCapacity(BigDecimal investmentCapacity) {
        this.investmentCapacity = investmentCapacity;
    }
    
    public BigDecimal getMinimumInvestmentAmount() {
        return minimumInvestmentAmount;
    }
    
    public void setMinimumInvestmentAmount(BigDecimal minimumInvestmentAmount) {
        this.minimumInvestmentAmount = minimumInvestmentAmount;
    }
    
    public BigDecimal getMaximumInvestmentAmount() {
        return maximumInvestmentAmount;
    }
    
    public void setMaximumInvestmentAmount(BigDecimal maximumInvestmentAmount) {
        this.maximumInvestmentAmount = maximumInvestmentAmount;
    }
    
    public String getPreferredSectors() {
        return preferredSectors;
    }
    
    public void setPreferredSectors(String preferredSectors) {
        this.preferredSectors = preferredSectors;
    }
    
    public Integer getInvestmentExperienceYears() {
        return investmentExperienceYears;
    }
    
    public void setInvestmentExperienceYears(Integer investmentExperienceYears) {
        this.investmentExperienceYears = investmentExperienceYears;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
    
    public String getRiskAppetite() {
        return riskAppetite;
    }
    
    public void setRiskAppetite(String riskAppetite) {
        this.riskAppetite = riskAppetite;
    }
    
    public BigDecimal getExpectedRoiPercentage() {
        return expectedRoiPercentage;
    }
    
    public void setExpectedRoiPercentage(BigDecimal expectedRoiPercentage) {
        this.expectedRoiPercentage = expectedRoiPercentage;
    }
    
    public String getPanCardUrl() {
        return panCardUrl;
    }
    
    public void setPanCardUrl(String panCardUrl) {
        this.panCardUrl = panCardUrl;
    }
    
    public String getAadharCardUrl() {
        return aadharCardUrl;
    }
    
    public void setAadharCardUrl(String aadharCardUrl) {
        this.aadharCardUrl = aadharCardUrl;
    }
    
    public String getBankStatementUrl() {
        return bankStatementUrl;
    }
    
    public void setBankStatementUrl(String bankStatementUrl) {
        this.bankStatementUrl = bankStatementUrl;
    }
    
    public String getInvestmentPortfolioUrl() {
        return investmentPortfolioUrl;
    }
    
    public void setInvestmentPortfolioUrl(String investmentPortfolioUrl) {
        this.investmentPortfolioUrl = investmentPortfolioUrl;
    }
}
