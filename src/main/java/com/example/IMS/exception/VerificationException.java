package com.example.IMS.exception;

import com.example.IMS.model.enums.VerificationType;

/**
 * Custom exception for verification failures
 */
public class VerificationException extends RuntimeException {
    
    private final VerificationType verificationType;
    private final String errorCode;
    
    public VerificationException(VerificationType verificationType, String message) {
        super(message);
        this.verificationType = verificationType;
        this.errorCode = "VERIFICATION_ERROR";
    }
    
    public VerificationException(VerificationType verificationType, String message, Throwable cause) {
        super(message, cause);
        this.verificationType = verificationType;
        this.errorCode = "VERIFICATION_ERROR";
    }
    
    public VerificationException(VerificationType verificationType, String errorCode, String message) {
        super(message);
        this.verificationType = verificationType;
        this.errorCode = errorCode;
    }
    
    public VerificationException(VerificationType verificationType, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.verificationType = verificationType;
        this.errorCode = errorCode;
    }
    
    public VerificationType getVerificationType() {
        return verificationType;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
