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
 * Bank Verification Service - Strategy Pattern Implementation
 * 
 * Performs "Penny Drop" verification by transferring ₹1 to validate account.
 * 
 * Supports both MOCK and REAL modes via configuration.
 * 
 * MOCK Mode:
 * - Returns success for valid format account numbers and IFSC codes
 * - No external API calls or actual money transfer
 * - Instant response for testing
 * 
 * REAL Mode:
 * - Integrates with Razorpay/Cashfree/PayU penny drop API
 * - Transfers ₹1 to validate account is active
 * - Returns account holder name for matching
 * 
 * Configuration:
 * verification.mode=MOCK|REAL
 * verification.bank.api.url=https://api.example.com/bank/pennydrop
 * verification.bank.api.key=your_api_key_here
 */
@Service
public class BankVerificationService implements VerificationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(BankVerificationService.class);
    
    @Autowired
    @Qualifier("verificationWebClient")
    private WebClient webClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${verification.mode:MOCK}")
    private VerificationMode verificationMode;
    
    @Value("${verification.bank.api.url:}")
    private String bankApiUrl;
    
    @Value("${verification.bank.api.key:}")
    private String bankApiKey;
    
    @Value("${verification.bank.timeout-ms:45000}")
    private long timeoutMs;  // Bank verification can take longer (money transfer)
    
    @Override
    public boolean supports(VerificationRequest request) {
        return request.getVerificationType() == VerificationType.BANK;
    }
    
    @Override
    public VerificationResult verify(VerificationRequest request) {
        if (!supports(request)) {
            throw new IllegalArgumentException("BankVerificationService does not support: " + request.getVerificationType());
        }
        
        String accountNumber = request.getParameter("accountNumber");
        String ifsc = request.getParameter("ifsc");
        String accountHolderName = request.getParameter("accountHolderName");
        
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return VerificationResult.failure(VerificationType.BANK, "Account number is required", "Missing accountNumber parameter");
        }
        
        if (ifsc == null || ifsc.trim().isEmpty()) {
            return VerificationResult.failure(VerificationType.BANK, "IFSC code is required", "Missing ifsc parameter");
        }
        
        // IFSC format validation (11 characters: 4-alpha bank code + 0 + 6-alphanumeric branch code)
        if (!isValidIfscFormat(ifsc)) {
            return VerificationResult.failure(VerificationType.BANK, "Invalid IFSC format", 
                "IFSC must be 11 characters: 4 letters + 0 + 6 alphanumeric (e.g., SBIN0001234)");
        }
        
        // Account number format validation (basic check: 9-18 digits)
        if (!isValidAccountNumberFormat(accountNumber)) {
            return VerificationResult.failure(VerificationType.BANK, "Invalid account number format", 
                "Account number must be 9-18 digits");
        }
        
        logger.info("Verifying bank account: {} with IFSC: {} in {} mode", 
            maskAccountNumber(accountNumber), ifsc, verificationMode);
        
        if (verificationMode == VerificationMode.MOCK) {
            return verifyMock(accountNumber, ifsc, accountHolderName);
        } else {
            return verifyReal(accountNumber, ifsc, accountHolderName);
        }
    }
    
    /**
     * Mock verification - returns success for valid format bank details
     */
    private VerificationResult verifyMock(String accountNumber, String ifsc, String accountHolderName) {
        logger.debug("MOCK: Verifying bank account: {} with IFSC: {}", maskAccountNumber(accountNumber), ifsc);
        
        // Simulate penny drop delay
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock logic: Account numbers ending with even digit are valid
        int lastDigit = Integer.parseInt(accountNumber.substring(accountNumber.length() - 1));
        if (lastDigit % 2 == 0) {
            VerificationResult result = VerificationResult.success(VerificationType.BANK, "Bank account verified successfully (MOCK)");
            result.addData("accountNumber", maskAccountNumber(accountNumber));
            result.addData("ifsc", ifsc);
            result.addData("accountHolderName", accountHolderName != null ? accountHolderName : "Mock Account Holder");
            result.addData("bankName", ifsc.substring(0, 4) + " Bank");
            result.addData("branchName", "Mock Branch");
            result.addData("accountStatus", "Active");
            result.addData("pennyDropAmount", "1.00");
            result.addData("mode", "MOCK");
            return result;
        } else {
            return VerificationResult.failure(VerificationType.BANK, "Bank account verification failed (MOCK)", 
                "Account not found or inactive");
        }
    }
    
    /**
     * Real verification - calls external penny drop API
     */
    private VerificationResult verifyReal(String accountNumber, String ifsc, String accountHolderName) {
        if (bankApiUrl == null || bankApiUrl.trim().isEmpty()) {
            logger.error("Bank API URL not configured. Set verification.bank.api.url in application.properties");
            throw new VerificationException(VerificationType.BANK, "API_NOT_CONFIGURED", 
                "Bank verification API URL is not configured");
        }
        
        if (bankApiKey == null || bankApiKey.trim().isEmpty()) {
            logger.warn("Bank API key not configured. API calls may fail.");
        }
        
        logger.info("REAL: Calling penny drop API for account: {}", maskAccountNumber(accountNumber));
        
        try {
            // Build request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("accountNumber", accountNumber);
            requestBody.put("ifsc", ifsc);
            if (accountHolderName != null) {
                requestBody.put("accountHolderName", accountHolderName);
            }
            
            // Call external API
            String responseBody = webClient.post()
                .uri(bankApiUrl)
                .header("X-API-Key", bankApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Check if verification succeeded (API-specific logic)
            boolean success = jsonResponse.has("verified") && jsonResponse.get("verified").asBoolean();
            
            if (success) {
                VerificationResult result = VerificationResult.success(VerificationType.BANK, "Bank account verified successfully");
                result.addData("accountNumber", maskAccountNumber(accountNumber));
                result.addData("ifsc", ifsc);
                result.addData("accountHolderName", jsonResponse.path("accountHolderName").asText());
                result.addData("bankName", jsonResponse.path("bankName").asText());
                result.addData("branchName", jsonResponse.path("branchName").asText());
                result.addData("accountStatus", jsonResponse.path("status").asText());
                result.addData("pennyDropAmount", jsonResponse.path("amount").asText("1.00"));
                result.addData("mode", "REAL");
                return result;
            } else {
                String errorMessage = jsonResponse.path("message").asText("Bank account verification failed");
                return VerificationResult.failure(VerificationType.BANK, errorMessage, responseBody);
            }
            
        } catch (WebClientResponseException e) {
            logger.error("Bank API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return VerificationResult.failure(VerificationType.BANK, "Bank account not found", e.getResponseBodyAsString());
            } else if (e.getStatusCode().is4xxClientError()) {
                return VerificationResult.failure(VerificationType.BANK, "Invalid request to Bank API", e.getMessage());
            } else {
                throw new VerificationException(VerificationType.BANK, "API_ERROR", 
                    "Bank API returned error: " + e.getStatusCode(), e);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during bank account verification", e);
            throw new VerificationException(VerificationType.BANK, "UNKNOWN_ERROR", 
                "Unexpected error during bank account verification", e);
        }
    }
    
    /**
     * Validate IFSC format (11 characters: 4 alpha + 0 + 6 alphanumeric)
     */
    private boolean isValidIfscFormat(String ifsc) {
        if (ifsc == null || ifsc.length() != 11) {
            return false;
        }
        // Regex: 4 uppercase letters + 0 + 6 alphanumeric
        return ifsc.matches("^[A-Z]{4}0[A-Z0-9]{6}$");
    }
    
    /**
     * Validate account number format (9-18 digits)
     */
    private boolean isValidAccountNumberFormat(String accountNumber) {
        if (accountNumber == null) {
            return false;
        }
        // Remove spaces/hyphens if any
        String cleaned = accountNumber.replaceAll("[\\s-]", "");
        // Check if 9-18 digits
        return cleaned.matches("^[0-9]{9,18}$");
    }
    
    /**
     * Mask account number for logging (show first 2 and last 4 digits)
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 7) {
            return "****";
        }
        return accountNumber.substring(0, 2) + "******" + accountNumber.substring(accountNumber.length() - 4);
    }
}
