package com.example.IMS.service.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.exception.VerificationException;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.VerificationLog;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.VerificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verification Orchestrator
 * 
 * Central service for coordinating verification execution, logging, and auto-triggering.
 * 
 * Responsibilities:
 * 1. Route verification requests to appropriate provider (Strategy pattern)
 * 2. Log all verification attempts to verification_logs table
 * 3. Mask sensitive data before logging
 * 4. Auto-trigger verifications when configured
 * 5. Provide manual retry capability
 * 
 * Configuration:
 * verification.auto=true|false - Enable/disable auto-trigger
 * verification.mode=MOCK|REAL - Use mock or real API providers
 */
@Service
@Transactional
public class VerificationOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(VerificationOrchestrator.class);
    
    @Autowired
    private GstVerificationService gstVerificationService;
    
    @Autowired
    private PanVerificationService panVerificationService;
    
    @Autowired
    private BankVerificationService bankVerificationService;
    
    @Autowired
    private CinVerificationService cinVerificationService;
    
    @Autowired
    private VerificationLogRepository verificationLogRepository;
    
    @Autowired
    private BusinessProfileRepository businessProfileRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${verification.auto:false}")
    private boolean autoVerification;
    
    /**
     * Execute verification request
     * 
     * @param request Verification request with type-specific parameters
     * @return VerificationResult with success flag and data
     */
    public VerificationResult executeVerification(VerificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("VerificationRequest cannot be null");
        }
        
        // Validate business profile exists
        BusinessProfile businessProfile = businessProfileRepository.findById(request.getBusinessProfileId())
            .orElseThrow(() -> new EntityNotFoundException("Business profile not found: " + request.getBusinessProfileId()));
        
        logger.info("Starting verification: type={}, businessProfileId={}", 
            request.getVerificationType(), request.getBusinessProfileId());
        
        long startTime = System.currentTimeMillis();
        VerificationResult result;
        
        try {
            // Route to appropriate provider
            VerificationProvider provider = getProviderForType(request.getVerificationType());
            
            // Execute verification
            result = provider.verify(request);
            
            long executionTime = System.currentTimeMillis() - startTime;
            result.addData("executionTimeMs", executionTime);
            
            logger.info("Verification completed: type={}, success={}, time={}ms", 
                request.getVerificationType(), result.isSuccess(), executionTime);
            
        } catch (VerificationException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Verification exception: type={}, error={}", request.getVerificationType(), e.getMessage());
            
            result = VerificationResult.failure(request.getVerificationType(), e.getMessage(), e.getErrorCode());
            result.addData("executionTimeMs", executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error during verification", e);
            
            result = VerificationResult.failure(request.getVerificationType(), 
                "Unexpected error: " + e.getMessage(), e.toString());
            result.addData("executionTimeMs", executionTime);
        }
        
        // Log verification attempt
        logVerificationAttempt(businessProfile, request, result);
        
        return result;
    }
    
    /**
     * Check if auto-verification is enabled
     */
    public boolean isAutoVerificationEnabled() {
        return autoVerification;
    }
    
    /**
     * Auto-trigger verification for a business profile
     * Only executes if verification.auto=true
     * 
     * @param businessProfileId Business profile ID
     * @return Map of verification type to result
     */
    public Map<VerificationType, VerificationResult> autoTriggerVerifications(Long businessProfileId) {
        Map<VerificationType, VerificationResult> results = new HashMap<>();
        
        if (!autoVerification) {
            logger.debug("Auto-verification disabled, skipping auto-trigger for businessProfileId={}", businessProfileId);
            return results;
        }
        
        logger.info("Auto-triggering verifications for businessProfileId={}", businessProfileId);
        
        BusinessProfile businessProfile = businessProfileRepository.findById(businessProfileId)
            .orElseThrow(() -> new EntityNotFoundException("Business profile not found: " + businessProfileId));
        
        // Trigger GST verification
        if (businessProfile.getGstin() != null && !businessProfile.getGstin().trim().isEmpty()) {
            try {
                VerificationRequest gstRequest = VerificationRequest.forGst(businessProfileId, businessProfile.getGstin());
                VerificationResult gstResult = executeVerification(gstRequest);
                results.put(VerificationType.GST, gstResult);
            } catch (Exception e) {
                logger.error("Auto-trigger GST verification failed", e);
            }
        }
        
        // Trigger PAN verification (decrypt PAN first)
        if (businessProfile.getPanNumber() != null && !businessProfile.getPanNumber().trim().isEmpty()) {
            try {
                VerificationRequest panRequest = VerificationRequest.forPan(businessProfileId, businessProfile.getPanNumber());
                VerificationResult panResult = executeVerification(panRequest);
                results.put(VerificationType.PAN, panResult);
            } catch (Exception e) {
                logger.error("Auto-trigger PAN verification failed", e);
            }
        }
        
        // Trigger CIN verification (if applicable)
        if (businessProfile.getCinNumber() != null && !businessProfile.getCinNumber().trim().isEmpty()) {
            try {
                VerificationRequest cinRequest = VerificationRequest.forCin(businessProfileId, businessProfile.getCinNumber());
                VerificationResult cinResult = executeVerification(cinRequest);
                results.put(VerificationType.CIN, cinResult);
            } catch (Exception e) {
                logger.error("Auto-trigger CIN verification failed", e);
            }
        }
        
        logger.info("Auto-trigger completed: businessProfileId={}, results={}", businessProfileId, results.size());
        return results;
    }
    
    /**
     * Manual retry verification
     * Always available regardless of auto-verification setting
     * 
     * @param businessProfileId Business profile ID
     * @param verificationType Verification type to retry
     * @return VerificationResult
     */
    public VerificationResult retryVerification(Long businessProfileId, VerificationType verificationType) {
        logger.info("Manual retry verification: businessProfileId={}, type={}", businessProfileId, verificationType);
        
        BusinessProfile businessProfile = businessProfileRepository.findById(businessProfileId)
            .orElseThrow(() -> new EntityNotFoundException("Business profile not found: " + businessProfileId));
        
        VerificationRequest request;
        
        switch (verificationType) {
            case GST:
                if (businessProfile.getGstin() == null || businessProfile.getGstin().trim().isEmpty()) {
                    return VerificationResult.failure(verificationType, "GSTIN not set on business profile", null);
                }
                request = VerificationRequest.forGst(businessProfileId, businessProfile.getGstin());
                break;
                
            case PAN:
                if (businessProfile.getPanNumber() == null || businessProfile.getPanNumber().trim().isEmpty()) {
                    return VerificationResult.failure(verificationType, "PAN not set on business profile", null);
                }
                request = VerificationRequest.forPan(businessProfileId, businessProfile.getPanNumber());
                break;
                
            case CIN:
                if (businessProfile.getCinNumber() == null || businessProfile.getCinNumber().trim().isEmpty()) {
                    return VerificationResult.failure(verificationType, "CIN not set on business profile", null);
                }
                request = VerificationRequest.forCin(businessProfileId, businessProfile.getCinNumber());
                break;
                
            default:
                return VerificationResult.failure(verificationType, "Unsupported verification type for manual retry", null);
        }
        
        return executeVerification(request);
    }
    
    /**
     * Get verification provider for a specific type
     */
    private VerificationProvider getProviderForType(VerificationType verificationType) {
        switch (verificationType) {
            case GST:
                return gstVerificationService;
            case PAN:
                return panVerificationService;
            case BANK:
                return bankVerificationService;
            case CIN:
                return cinVerificationService;
            default:
                throw new IllegalArgumentException("No provider found for verification type: " + verificationType);
        }
    }
    
    /**
     * Log verification attempt to database
     * Masks sensitive data before storage
     */
    private void logVerificationAttempt(BusinessProfile businessProfile, VerificationRequest request, VerificationResult result) {
        try {
            VerificationLog log = new VerificationLog();
            log.setBusinessProfile(businessProfile);
            log.setVerificationType(request.getVerificationType());
            log.setVerificationResult(result.getResultCode());
            // Note: verificationResult stores the code (SUCCESS/FAILED), errorMessage stores details
            log.setCreatedAt(LocalDateTime.now());
            
            // Mask sensitive data before storing
            Map<String, String> maskedRequest = maskRequestData(request.getParameters());
            log.setRequestPayload(objectMapper.writeValueAsString(maskedRequest));
            
            Map<String, Object> maskedResponse = maskResponseData(result.getData());
            log.setResponsePayload(objectMapper.writeValueAsString(maskedResponse));
            
            // Store execution time
            if (result.getData().containsKey("executionTimeMs")) {
                log.setExecutionTimeMs((Long) result.getData().get("executionTimeMs"));
            }

            // Store HTTP status code
            if (result.getData().containsKey("httpStatusCode")) {
                Object statusCode = result.getData().get("httpStatusCode");
                if (statusCode instanceof Integer) {
                    log.setHttpStatusCode((Integer) statusCode);
                } else if (statusCode instanceof Number) {
                    log.setHttpStatusCode(((Number) statusCode).intValue());
                }
            }
            
            // Store error details if failed
            if (!result.isSuccess() && result.getErrorDetails() != null) {
                // Truncate error details to avoid excessive storage
                String errorDetails = result.getErrorDetails();
                if (errorDetails.length() > 5000) {
                    errorDetails = errorDetails.substring(0, 5000) + "... [truncated]";
                }
                log.setErrorMessage(errorDetails);
            }
            
            verificationLogRepository.save(log);
            logger.debug("Verification attempt logged: id={}, type={}, result={}", 
                log.getId(), log.getVerificationType(), log.getVerificationResult());
            
        } catch (Exception e) {
            // Don't fail verification if logging fails
            logger.error("Failed to log verification attempt", e);
        }
    }
    
    /**
     * Mask sensitive data in request parameters
     */
    private Map<String, String> maskRequestData(Map<String, String> data) {
        if (data == null) {
            return new HashMap<>();
        }
        
        Map<String, String> masked = new HashMap<>(data);
        
        // Mask account numbers
        if (masked.containsKey("accountNumber")) {
            String accountNumber = masked.get("accountNumber");
            masked.put("accountNumber", maskAccountNumber(accountNumber));
        }
        
        // Mask PAN (partial masking - show first 2 and last 1)
        if (masked.containsKey("pan")) {
            String pan = masked.get("pan");
            masked.put("pan", maskPan(pan));
        }
        
        return masked;
    }
    
    /**
     * Mask sensitive data in response data
     */
    private Map<String, Object> maskResponseData(Map<String, Object> data) {
        if (data == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> masked = new HashMap<>(data);
        
        // Mask account numbers
        if (masked.containsKey("accountNumber") && masked.get("accountNumber") instanceof String) {
            String accountNumber = (String) masked.get("accountNumber");
            masked.put("accountNumber", maskAccountNumber(accountNumber));
        }
        
        return masked;
    }
    
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 7) {
            return "****";
        }
        return accountNumber.substring(0, 2) + "******" + accountNumber.substring(accountNumber.length() - 4);
    }
    
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) {
            return "***";
        }
        return pan.substring(0, 2) + "******" + pan.substring(pan.length() - 1);
    }
    
    /**
     * Get verification history for a business profile
     */
    public List<VerificationLog> getVerificationHistory(Long businessProfileId) {
        return verificationLogRepository.findByBusinessProfileId(businessProfileId);
    }
    
    /**
     * Get latest successful verification for a type
     */
    public VerificationLog getLatestSuccessfulVerification(Long businessProfileId, VerificationType verificationType) {
        List<VerificationLog> logs = verificationLogRepository.findLastSuccessfulVerification(
            businessProfileId, 
            verificationType,
            PageRequest.of(0, 1) // Get first result only
        );
        return logs.isEmpty() ? null : logs.get(0);
    }
}
