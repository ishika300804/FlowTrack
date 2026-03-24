package com.example.IMS.dto.verification;

import com.example.IMS.model.enums.VerificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Common verification result DTO
 */
public class VerificationResult {
    
    private VerificationType verificationType;
    private boolean success;
    private String resultCode;          // API-specific result code
    private String message;              // Human-readable message
    private Map<String, Object> data;    // Additional data from verification
    private String errorDetails;         // Error stack/details (if failed)
    private LocalDateTime timestamp;
    
    public VerificationResult() {
        this.data = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    public VerificationResult(VerificationType verificationType, boolean success, String message) {
        this();
        this.verificationType = verificationType;
        this.success = success;
        this.message = message;
    }
    
    // Builder methods for success
    public static VerificationResult success(VerificationType type, String message) {
        VerificationResult result = new VerificationResult(type, true, message);
        result.setResultCode("SUCCESS");
        return result;
    }
    
    // Builder methods for failure
    public static VerificationResult failure(VerificationType type, String message, String errorDetails) {
        VerificationResult result = new VerificationResult(type, false, message);
        result.setResultCode("FAILED");
        result.setErrorDetails(errorDetails);
        return result;
    }
    
    // Builder methods for timeout
    public static VerificationResult timeout(VerificationType type) {
        VerificationResult result = new VerificationResult(type, false, "Verification request timed out");
        result.setResultCode("TIMEOUT");
        return result;
    }
    
    // Add data
    public VerificationResult addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
    
    // Getters and setters
    public VerificationType getVerificationType() {
        return verificationType;
    }
    
    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getResultCode() {
        return resultCode;
    }
    
    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
