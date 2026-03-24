package com.example.IMS.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Entity
@Table(name = "investor_profiles")
public class InvestorProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @NotBlank(message = "Investor name is required")
    @Column(name = "investor_name", nullable = false)
    private String investorName; // Individual or Company name
    
    @NotBlank(message = "Investor type is required")
    @Column(name = "investor_type", nullable = false)
    private String investorType; // Individual, Angel Investor, Venture Capital, Private Equity, etc.
    
    @Column(name = "pan_number")
    private String panNumber;
    
    @Column(name = "aadhar_number")
    private String aadharNumber; // For individual investors
    
    @Column(name = "company_registration_number")
    private String companyRegistrationNumber; // For institutional investors
    
    @Column(name = "investment_capacity")
    private BigDecimal investmentCapacity; // Available capital for investment
    
    @Column(name = "minimum_investment_amount")
    private BigDecimal minimumInvestmentAmount;
    
    @Column(name = "maximum_investment_amount")
    private BigDecimal maximumInvestmentAmount;
    
    @Column(name = "preferred_sectors", columnDefinition = "TEXT")
    private String preferredSectors; // Retail, F&B, E-commerce, etc. (JSON or comma-separated)
    
    @Column(name = "investment_experience_years")
    private Integer investmentExperienceYears;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "bank_account_number")
    private String bankAccountNumber;
    
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "bank_ifsc_code")
    private String bankIfscCode;
    
    @Column(name = "pan_card_url")
    private String panCardUrl; // Document upload path
    
    @Column(name = "aadhar_card_url")
    private String aadharCardUrl; // Document upload path
    
    @Column(name = "bank_statement_url")
    private String bankStatementUrl; // Document upload path
    
    @Column(name = "investment_portfolio_url")
    private String investmentPortfolioUrl; // Optional: Previous investment proof
    
    @Column(name = "verification_status")
    private String verificationStatus = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "risk_appetite")
    private String riskAppetite; // LOW, MEDIUM, HIGH
    
    @Column(name = "expected_roi_percentage")
    private BigDecimal expectedRoiPercentage;
    
    // Constructors
    public InvestorProfile() {}
    
    public InvestorProfile(User user, String investorName, String investorType) {
        this.user = user;
        this.investorName = investorName;
        this.investorType = investorType;
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
}
