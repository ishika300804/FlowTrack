package com.example.IMS.service.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.exception.VerificationException;
import com.example.IMS.model.enums.VerificationMode;
import com.example.IMS.model.enums.VerificationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * GST Verification Service - Strategy Pattern Implementation
 * 
 * Supports both MOCK and REAL modes via configuration.
 * 
 * MOCK Mode:
 * - Returns predefined success/failure based on GSTIN pattern
 * - No external API calls
 * - Instant response for testing
 * 
 * REAL Mode:
 * - Integrates with GSTN API (or third-party GST verification provider)
 * - Validates GSTIN format and active status
 * - Returns business name, address, registration date
 * 
 * Configuration:
 * verification.mode=MOCK|REAL
 * verification.gst.api.url=https://api.example.com/gst/verify
 * verification.gst.api.key=your_api_key_here
 */
@Service
public class GstVerificationService implements VerificationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(GstVerificationService.class);
    
    @Autowired
    @Qualifier("verificationWebClient")
    private WebClient webClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${verification.mode:MOCK}")
    private VerificationMode verificationMode;
    
    @Value("${verification.gst.api.url:}")
    private String gstApiUrl;
    
    @Value("${verification.gst.api.key:}")
    private String gstApiKey;
    
    @Value("${verification.gst.timeout-ms:30000}")
    private long timeoutMs;
    
    @Override
    public boolean supports(VerificationRequest request) {
        return request.getVerificationType() == VerificationType.GST;
    }
    
    @Override
    public VerificationResult verify(VerificationRequest request) {
        if (!supports(request)) {
            throw new IllegalArgumentException("GstVerificationService does not support: " + request.getVerificationType());
        }
        
        String gstin = request.getParameter("gstin");
        if (gstin == null || gstin.trim().isEmpty()) {
            return VerificationResult.failure(VerificationType.GST, "GSTIN is required", "Missing GSTIN parameter");
        }
        
        // Format validation (15 characters: 2-state + 10-PAN + 1-entity + 1-Z + 1-checksum)
        if (!isValidGstinFormat(gstin)) {
            return VerificationResult.failure(VerificationType.GST, "Invalid GSTIN format", 
                "GSTIN must be 15 characters: 2-digit state code + 10-digit PAN + 3-digit entity/Z/checksum");
        }
        
        logger.info("Verifying GSTIN: {} in {} mode", maskGstin(gstin), verificationMode);
        
        if (verificationMode == VerificationMode.MOCK) {
            return verifyMock(gstin);
        } else {
            return verifyReal(gstin);
        }
    }
    
    /**
     * Mock verification - returns success for valid format GST numbers
     */
    private VerificationResult verifyMock(String gstin) {
        logger.debug("MOCK: Verifying GSTIN: {}", maskGstin(gstin));
        
        // Simulate API delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock logic: GST numbers starting with "29" (Karnataka) are valid
        if (gstin.startsWith("29")) {
            VerificationResult result = VerificationResult.success(VerificationType.GST, "GSTIN verified successfully (MOCK)");
            result.addData("gstin", gstin);
            result.addData("businessName", "Mock Business Name Pvt Ltd");
            result.addData("registrationDate", "2020-01-01");
            result.addData("businessAddress", "Mock Address, Karnataka");
            result.addData("status", "Active");
            result.addData("mode", "MOCK");
            return result;
        } else {
            return VerificationResult.failure(VerificationType.GST, "GSTIN verification failed (MOCK)", 
                "GSTIN not found or inactive in GSTN database");
        }
    }
    
    /**
     * Real verification - calls external GSTN API
     */
    private VerificationResult verifyReal(String gstin) {
        if (gstApiUrl == null || gstApiUrl.trim().isEmpty()) {
            logger.error("GST API URL not configured. Set verification.gst.api.url in application.properties");
            throw new VerificationException(VerificationType.GST, "API_NOT_CONFIGURED", 
                "GST verification API URL is not configured");
        }
        
        if (gstApiKey == null || gstApiKey.trim().isEmpty()) {
            logger.warn("GST API key not configured. API calls may fail.");
        }
        
        logger.info("REAL: Calling GST API for GSTIN: {}", maskGstin(gstin));
        
        try {
            // Build request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("gstin", gstin);
            
            // Call external API
            String responseBody = webClient.post()
                .uri(gstApiUrl)
                .header("X-API-Key", gstApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Check if verification succeeded (API-specific logic)
            boolean success = jsonResponse.has("valid") && jsonResponse.get("valid").asBoolean();
            
            if (success) {
                VerificationResult result = VerificationResult.success(VerificationType.GST, "GSTIN verified successfully");
                result.addData("gstin", gstin);
                result.addData("businessName", jsonResponse.path("businessName").asText());
                result.addData("registrationDate", jsonResponse.path("registrationDate").asText());
                result.addData("businessAddress", jsonResponse.path("address").asText());
                result.addData("status", jsonResponse.path("status").asText());
                result.addData("mode", "REAL");
                return result;
            } else {
                String errorMessage = jsonResponse.path("message").asText("GSTIN verification failed");
                return VerificationResult.failure(VerificationType.GST, errorMessage, responseBody);
            }
            
        } catch (WebClientResponseException e) {
            logger.error("GST API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return VerificationResult.failure(VerificationType.GST, "GSTIN not found", e.getResponseBodyAsString());
            } else if (e.getStatusCode().is4xxClientError()) {
                return VerificationResult.failure(VerificationType.GST, "Invalid request to GST API", e.getMessage());
            } else {
                throw new VerificationException(VerificationType.GST, "API_ERROR", 
                    "GST API returned error: " + e.getStatusCode(), e);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during GST verification", e);
            throw new VerificationException(VerificationType.GST, "UNKNOWN_ERROR", 
                "Unexpected error during GST verification", e);
        }
    }
    
    /**
     * Validate GSTIN format (15 characters)
     */
    private boolean isValidGstinFormat(String gstin) {
        if (gstin == null || gstin.length() != 15) {
            return false;
        }
        // Basic regex: 2 digits (state) + 10 alphanumeric (PAN) + 1 alpha + 1 'Z' + 1 alphanumeric
        return gstin.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    }
    
    /**
     * Mask GSTIN for logging (show first 4 and last 2 characters)
     */
    private String maskGstin(String gstin) {
        if (gstin == null || gstin.length() < 7) {
            return "***";
        }
        return gstin.substring(0, 4) + "******" + gstin.substring(gstin.length() - 2);
    }
}
