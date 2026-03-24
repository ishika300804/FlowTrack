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
 * CIN Verification Service - Strategy Pattern Implementation
 * 
 * Supports both MOCK and REAL modes via configuration.
 * 
 * MOCK Mode:
 * - Returns success for valid format CIN numbers
 * - No external API calls
 * - Instant response for testing
 * 
 * REAL Mode:
 * - Integrates with MCA (Ministry of Corporate Affairs) API
 * - Validates CIN format and company status
 * - Returns company name, registration date, directors list
 * 
 * Configuration:
 * verification.mode=MOCK|REAL
 * verification.cin.api.url=https://api.example.com/cin/verify
 * verification.cin.api.key=your_api_key_here
 */
@Service
public class CinVerificationService implements VerificationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(CinVerificationService.class);
    
    @Autowired
    @Qualifier("verificationWebClient")
    private WebClient webClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${verification.mode:MOCK}")
    private VerificationMode verificationMode;
    
    @Value("${verification.cin.api.url:}")
    private String cinApiUrl;
    
    @Value("${verification.cin.api.key:}")
    private String cinApiKey;
    
    @Value("${verification.cin.timeout-ms:30000}")
    private long timeoutMs;
    
    @Override
    public boolean supports(VerificationRequest request) {
        return request.getVerificationType() == VerificationType.CIN;
    }
    
    @Override
    public VerificationResult verify(VerificationRequest request) {
        if (!supports(request)) {
            throw new IllegalArgumentException("CinVerificationService does not support: " + request.getVerificationType());
        }
        
        String cin = request.getParameter("cin");
        if (cin == null || cin.trim().isEmpty()) {
            return VerificationResult.failure(VerificationType.CIN, "CIN is required", "Missing CIN parameter");
        }
        
        // Format validation (21 characters)
        if (!isValidCinFormat(cin)) {
            return VerificationResult.failure(VerificationType.CIN, "Invalid CIN format", 
                "CIN must be 21 characters (e.g., U74999MH2020PTC123456)");
        }
        
        logger.info("Verifying CIN: {} in {} mode", maskCin(cin), verificationMode);
        
        if (verificationMode == VerificationMode.MOCK) {
            return verifyMock(cin);
        } else {
            return verifyReal(cin);
        }
    }
    
    /**
     * Mock verification - returns success for valid format CIN numbers
     */
    private VerificationResult verifyMock(String cin) {
        logger.debug("MOCK: Verifying CIN: {}", maskCin(cin));
        
        // Simulate API delay
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock logic: CINs containing "PTC" (Private Company) are valid
        if (cin.contains("PTC") || cin.contains("PLC")) {
            VerificationResult result = VerificationResult.success(VerificationType.CIN, "CIN verified successfully (MOCK)");
            result.addData("cin", cin);
            result.addData("companyName", "Mock Private Limited Company");
            result.addData("registrationDate", "2020-01-15");
            result.addData("companyStatus", "Active");
            result.addData("companyCategory", "Company limited by shares");
            result.addData("companyClass", "Private");
            result.addData("authorizedCapital", "1000000");
            result.addData("paidUpCapital", "500000");
            result.addData("mode", "MOCK");
            return result;
        } else {
            return VerificationResult.failure(VerificationType.CIN, "CIN verification failed (MOCK)", 
                "CIN not found in MCA database");
        }
    }
    
    /**
     * Real verification - calls external MCA API
     */
    private VerificationResult verifyReal(String cin) {
        if (cinApiUrl == null || cinApiUrl.trim().isEmpty()) {
            logger.error("CIN API URL not configured. Set verification.cin.api.url in application.properties");
            throw new VerificationException(VerificationType.CIN, "API_NOT_CONFIGURED", 
                "CIN verification API URL is not configured");
        }
        
        if (cinApiKey == null || cinApiKey.trim().isEmpty()) {
            logger.warn("CIN API key not configured. API calls may fail.");
        }
        
        logger.info("REAL: Calling MCA API for CIN: {}", maskCin(cin));
        
        try {
            // Build request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("cin", cin);
            
            // Call external API
            String responseBody = webClient.post()
                .uri(cinApiUrl)
                .header("X-API-Key", cinApiKey)
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
                VerificationResult result = VerificationResult.success(VerificationType.CIN, "CIN verified successfully");
                result.addData("cin", cin);
                result.addData("companyName", jsonResponse.path("companyName").asText());
                result.addData("registrationDate", jsonResponse.path("registrationDate").asText());
                result.addData("companyStatus", jsonResponse.path("status").asText());
                result.addData("companyCategory", jsonResponse.path("companyCategory").asText());
                result.addData("companyClass", jsonResponse.path("companyClass").asText());
                result.addData("authorizedCapital", jsonResponse.path("authorizedCapital").asText());
                result.addData("paidUpCapital", jsonResponse.path("paidUpCapital").asText());
                result.addData("mode", "REAL");
                return result;
            } else {
                String errorMessage = jsonResponse.path("message").asText("CIN verification failed");
                return VerificationResult.failure(VerificationType.CIN, errorMessage, responseBody);
            }
            
        } catch (WebClientResponseException e) {
            logger.error("CIN API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return VerificationResult.failure(VerificationType.CIN, "CIN not found", e.getResponseBodyAsString());
            } else if (e.getStatusCode().is4xxClientError()) {
                return VerificationResult.failure(VerificationType.CIN, "Invalid request to CIN API", e.getMessage());
            } else {
                throw new VerificationException(VerificationType.CIN, "API_ERROR", 
                    "CIN API returned error: " + e.getStatusCode(), e);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during CIN verification", e);
            throw new VerificationException(VerificationType.CIN, "UNKNOWN_ERROR", 
                "Unexpected error during CIN verification", e);
        }
    }
    
    /**
     * Validate CIN format (21 characters)
     * Format: L/U + 5-digit activity code + 2-letter state + 4-digit year + PTC/PLC + 6-digit registration number
     * Example: U74999MH2020PTC123456
     */
    private boolean isValidCinFormat(String cin) {
        if (cin == null || cin.length() != 21) {
            return false;
        }
        // Regex: [LU] + 5 digits + 2 letters + 4 digits + (PTC|PLC|FLC|GAZ|NPG|OPC) + 6 digits
        return cin.matches("^[LU][0-9]{5}[A-Z]{2}[0-9]{4}(PTC|PLC|FLC|GAZ|NPG|OPC)[0-9]{6}$");
    }
    
    /**
     * Mask CIN for logging (show first 5 and last 4 characters)
     */
    private String maskCin(String cin) {
        if (cin == null || cin.length() < 10) {
            return "***";
        }
        return cin.substring(0, 5) + "**********" + cin.substring(cin.length() - 4);
    }
}
