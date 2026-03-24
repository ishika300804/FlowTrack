package com.example.IMS.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class PaymentRequest {
    
    @Min(value = 1, message = "Amount must be greater than 0")
    private double amount;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    private String receipt;
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @NotBlank(message = "Customer email is required")
    private String customerEmail;
    
    private String customerPhone;
    
    private String description;
    
    // Constructors
    public PaymentRequest() {
        this.currency = "INR";
    }
    
    public PaymentRequest(double amount, String customerName, String customerEmail) {
        this.amount = amount;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.currency = "INR";
    }
    
    // Getters and Setters
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getReceipt() {
        return receipt;
    }
    
    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
