package com.example.IMS.dto.verification;

import com.example.IMS.model.enums.VerificationType;

import java.util.HashMap;
import java.util.Map;

/**
 * Common verification request DTO
 * Uses Strategy pattern - different services populate different fields
 */
public class VerificationRequest {
    
    private VerificationType verificationType;
    private Long businessProfileId;
    private Map<String, String> parameters; // Flexible parameter map for different verification types
    
    public VerificationRequest() {
        this.parameters = new HashMap<>();
    }
    
    public VerificationRequest(VerificationType verificationType, Long businessProfileId) {
        this.verificationType = verificationType;
        this.businessProfileId = businessProfileId;
        this.parameters = new HashMap<>();
    }
    
    // Builder-style setters
    public VerificationRequest addParameter(String key, String value) {
        this.parameters.put(key, value);
        return this;
    }
    
    // GST-specific builder
    public static VerificationRequest forGst(Long businessProfileId, String gstin) {
        VerificationRequest request = new VerificationRequest(VerificationType.GST, businessProfileId);
        request.addParameter("gstin", gstin);
        return request;
    }
    
    // PAN-specific builder
    public static VerificationRequest forPan(Long businessProfileId, String pan) {
        VerificationRequest request = new VerificationRequest(VerificationType.PAN, businessProfileId);
        request.addParameter("pan", pan);
        return request;
    }
    
    // Bank-specific builder
    public static VerificationRequest forBank(Long businessProfileId, Long bankDetailsId, 
                                               String accountNumber, String ifsc, String accountHolderName) {
        VerificationRequest request = new VerificationRequest(VerificationType.BANK, businessProfileId);
        request.addParameter("bankDetailsId", bankDetailsId.toString());
        request.addParameter("accountNumber", accountNumber);
        request.addParameter("ifsc", ifsc);
        request.addParameter("accountHolderName", accountHolderName);
        return request;
    }
    
    // CIN-specific builder
    public static VerificationRequest forCin(Long businessProfileId, String cin) {
        VerificationRequest request = new VerificationRequest(VerificationType.CIN, businessProfileId);
        request.addParameter("cin", cin);
        return request;
    }
    
    // Getters and setters
    public VerificationType getVerificationType() {
        return verificationType;
    }
    
    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
    }
    
    public Long getBusinessProfileId() {
        return businessProfileId;
    }
    
    public void setBusinessProfileId(Long businessProfileId) {
        this.businessProfileId = businessProfileId;
    }
    
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
    public String getParameter(String key) {
        return parameters.get(key);
    }
}
