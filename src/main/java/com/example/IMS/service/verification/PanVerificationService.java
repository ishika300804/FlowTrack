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
 * PAN Verification Service - Strategy Pattern Implementation
 * 
 * Supports both MOCK and REAL modes via configuration.
 * 
 * MOCK Mode:
 * - Returns success for valid format PAN numbers
 * - No external API calls
 * - Instant response for testing
 * 
 * REAL Mode:
 * - Integrates with NSDL/Protean API or third-party PAN verification provider
 * - Validates PAN format and active status
 * - Returns holder name for matching
 * 
 * Configuration:
 * verification.mode=MOCK|REAL
 * verification.pan.api.url=https://api.example.com/pan/verify
 * verification.pan.api.key=your_api_key_here
 */
@Service
public class PanVerificationService implements VerificationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(PanVerificationService.class);
    
    @Autowired
    @Qualifier("verificationWebClient")
    private WebClient webClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${verification.mode:MOCK}")
    private VerificationMode verificationMode;
    
    @Value("${verification.pan.api.url:}")
    private String panApiUrl;
    
    @Value("${verification.pan.api.key:}")
    private String panApiKey;
    
    @Value("${verification.pan.timeout-ms:30000}")
    private long timeoutMs;
    
    @Override
    public boolean supports(VerificationRequest request) {
        return request.getVerificationType() == VerificationType.PAN;
    }
    
    @Override
    public VerificationResult verify(VerificationRequest request) {
        if (!supports(request)) {
            throw new IllegalArgumentException("PanVerificationService does not support: " + request.getVerificationType());
        }
        
        String pan = request.getParameter("pan");
        if (pan == null || pan.trim().isEmpty()) {
            return VerificationResult.failure(VerificationType.PAN, "PAN is required", "Missing PAN parameter");
        }
        
        // Format validation (10 characters: 5-alpha + 4-digit + 1-alpha)
        if (!isValidPanFormat(pan)) {
            return VerificationResult.failure(VerificationType.PAN, "Invalid PAN format", 
                "PAN must be 10 characters: 5 letters + 4 digits + 1 letter (e.g., ABCDE1234F)");
        }
        
        logger.info("Verifying PAN: {} in {} mode", maskPan(pan), verificationMode);
        
        if (verificationMode == VerificationMode.MOCK) {
            return verifyMock(pan);
        } else {
            return verifyReal(pan);
        }
    }
    
    /**
     * Mock verification - returns success for valid format PAN numbers
     */
    private VerificationResult verifyMock(String pan) {
        logger.debug("MOCK: Verifying PAN: {}", maskPan(pan));
        
        // Simulate API delay
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock logic: PANs ending with 'F' (firms/companies) are valid
        if (pan.endsWith("F") || pan.endsWith("C")) {
            VerificationResult result = VerificationResult.success(VerificationType.PAN, "PAN verified successfully (MOCK)");
            result.addData("pan", pan);
            result.addData("holderName", "Mock Holder Name");
            result.addData("status", "Active");
            result.addData("category", pan.endsWith("F") ? "Firm" : "Company");
            result.addData("mode", "MOCK");
            return result;
        } else {
            return VerificationResult.failure(VerificationType.PAN, "PAN verification failed (MOCK)", 
                "PAN not found or inactive");
        }
    }
    
    /**
     * Real verification - calls external NSDL/Protean API
     */
    private VerificationResult verifyReal(String pan) {
        if (panApiUrl == null || panApiUrl.trim().isEmpty()) {
            logger.error("PAN API URL not configured. Set verification.pan.api.url in application.properties");
            throw new VerificationException(VerificationType.PAN, "API_NOT_CONFIGURED", 
                "PAN verification API URL is not configured");
        }
        
        if (panApiKey == null || panApiKey.trim().isEmpty()) {
            logger.warn("PAN API key not configured. API calls may fail.");
        }
        
        logger.info("REAL: Calling PAN API for PAN: {}", maskPan(pan));
        
        try {
            // Build request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("pan", pan);
            
            // Call external API
            String responseBody = webClient.post()
                .uri(panApiUrl)
                .header("X-API-Key", panApiKey)
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
                VerificationResult result = VerificationResult.success(VerificationType.PAN, "PAN verified successfully");
                result.addData("pan", pan);
                result.addData("holderName", jsonResponse.path("holderName").asText());
                result.addData("status", jsonResponse.path("status").asText());
                result.addData("category", jsonResponse.path("category").asText());
                result.addData("mode", "REAL");
                return result;
            } else {
                String errorMessage = jsonResponse.path("message").asText("PAN verification failed");
                return VerificationResult.failure(VerificationType.PAN, errorMessage, responseBody);
            }
            
        } catch (WebClientResponseException e) {
            logger.error("PAN API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return VerificationResult.failure(VerificationType.PAN, "PAN not found", e.getResponseBodyAsString());
            } else if (e.getStatusCode().is4xxClientError()) {
                return VerificationResult.failure(VerificationType.PAN, "Invalid request to PAN API", e.getMessage());
            } else {
                throw new VerificationException(VerificationType.PAN, "API_ERROR", 
                    "PAN API returned error: " + e.getStatusCode(), e);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during PAN verification", e);
            throw new VerificationException(VerificationType.PAN, "UNKNOWN_ERROR", 
                "Unexpected error during PAN verification", e);
        }
    }
    
    /**
     * Validate PAN format (10 characters: 5 alpha + 4 digits + 1 alpha)
     */
    private boolean isValidPanFormat(String pan) {
        if (pan == null || pan.length() != 10) {
            return false;
        }
        // Regex: 5 uppercase letters + 4 digits + 1 uppercase letter
        return pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    }
    
    /**
     * Mask PAN for logging (show first 2 and last 1 character)
     */
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) {
            return "***";
        }
        return pan.substring(0, 2) + "******" + pan.substring(pan.length() - 1);
    }
}
