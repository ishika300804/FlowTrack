package com.example.IMS.service.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;

/**
 * Strategy Pattern Interface for Verification Services
 * 
 * All verification providers (GST, PAN, Bank, CIN) implement this interface.
 * Allows pluggable verification modules with consistent API.
 * 
 * Implementations:
 * - GstVerificationService (Mock + Real)
 * - PanVerificationService (Mock + Real)
 * - BankVerificationService (Mock + Real)
 * - CinVerificationService (Mock + Real)
 */
public interface VerificationProvider {
    
    /**
     * Execute verification request
     * 
     * @param request Verification request with type-specific parameters
     * @return VerificationResult with success flag and data
     * @throws com.example.IMS.exception.VerificationException if verification fails critically
     */
    VerificationResult verify(VerificationRequest request);
    
    /**
     * Check if this provider supports the given verification type
     * 
     * @param request Verification request
     * @return true if this provider can handle the request
     */
    boolean supports(VerificationRequest request);
}
