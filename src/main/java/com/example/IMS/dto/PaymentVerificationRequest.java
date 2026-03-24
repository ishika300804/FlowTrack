package com.example.IMS.dto;

import java.util.Map;

public class PaymentVerificationRequest {
    
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private Map<String, String> additionalData;
    
    // Constructors
    public PaymentVerificationRequest() {
    }
    
    public PaymentVerificationRequest(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpaySignature = razorpaySignature;
    }
    
    // Getters and Setters
    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }
    
    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }
    
    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }
    
    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }
    
    public String getRazorpaySignature() {
        return razorpaySignature;
    }
    
    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }
    
    public Map<String, String> getAdditionalData() {
        return additionalData;
    }
    
    public void setAdditionalData(Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }
}
