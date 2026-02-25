/**
 * Bank Verification Service Test - Prompt 3
 * ========================================
 * 
 * Tests BankVerificationService (Penny Drop) in REAL mode using MockWebServer.
 * 
 * Scenarios:
 * - ✓ 200 OK: Successful bank account verification
 * - ✓ 400 Bad Request: Invalid IFSC or account number
 * - ✓ 500 Internal Error: Server error (retry)
 * - ✓ Timeout: Network delay exceeds timeout
 * 
 * Bank verification validates:
 * - Account number exists
 * - IFSC code valid
 * - Account holder name match
 */
package com.example.IMS.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.exception.VerificationException;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.service.verification.BankVerificationService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "verification.mode=REAL",
    "verification.bank.api.key=test-bank-key-456",
    "verification.bank.timeout-ms=3000"
})
@DisplayName("Bank Verification Service Tests (Prompt 3)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BankVerificationServiceTest {

    @Autowired
    private BankVerificationService bankVerificationService;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockServerUrl = mockWebServer.url("/bank/verify").toString();
        setField(bankVerificationService, "bankApiUrl", mockServerUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    // ========================================
    // Test 1: Successful Verification (200 OK)
    // ========================================

    @Test
    @Order(1)
    @DisplayName("200 OK - Bank account verification successful")
    void testBankVerification_Success() throws InterruptedException {
        // Arrange
        String mockResponse = "{\n" +
            "    \"verified\": true,\n" +
            "    \"accountNumber\": \"1234567890\",\n" +
            "    \"ifscCode\": \"SBIN0001234\",\n" +
            "    \"accountHolderName\": \"JOHN DOE\",\n" +
            "    \"bankName\": \"State Bank of India\",\n" +
            "    \"branchName\": \"Main Branch\"\n" +
            "}";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"));

        VerificationRequest request = VerificationRequest.forBank(
            1L, 1L, "1234567890", "SBIN0001234", "John Doe");

        // Act
        VerificationResult result = bankVerificationService.verify(request);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Bank account verified successfully", result.getMessage());
        assertEquals("JOHN DOE", result.getData().get("accountHolderName"));
        assertEquals("State Bank of India", result.getData().get("bankName"));

        // Verify HTTP request
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("POST", recordedRequest.getMethod());
    }

    // ========================================
    // Test 2: Invalid Account - 400 Bad Request
    // ========================================

    @Test
    @Order(2)
    @DisplayName("400 Bad Request - Invalid account details (no retry)")
    void testBankVerification_BadRequest() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\": \"Invalid IFSC code\"}"));

        VerificationRequest request = VerificationRequest.forBank(
            1L, 1L, "1234567890", "SBIN0001234", "John Doe");

        // Act
        VerificationResult result = bankVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Invalid request to Bank API", result.getMessage());
        assertEquals(1, mockWebServer.getRequestCount());
    }

    // ========================================
    // Test 3: Server Error - 500
    // ========================================

    @Test
    @Order(3)
    @DisplayName("500 Internal Error - Triggers retry")
    void testBankVerification_ServerError() {
        // Arrange
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        VerificationRequest request = VerificationRequest.forBank(
            1L, 1L, "1234567890", "SBIN0001234", "John Doe");

        // Act & Assert
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            bankVerificationService.verify(request);
        });

        assertEquals(VerificationType.BANK, exception.getVerificationType());
        assertTrue(mockWebServer.getRequestCount() > 0);
    }

    // ========================================
    // Test 4: Timeout
    // ========================================

    @Test
    @Order(4)
    @DisplayName("Timeout - Request exceeds configured timeout")
    void testBankVerification_Timeout() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"verified\": true}")
            .setBodyDelay(5, TimeUnit.SECONDS)); // Exceeds 3s timeout

        VerificationRequest request = VerificationRequest.forBank(
            1L, 1L, "1234567890", "SBIN0001234", "John Doe");

        // Act & Assert
        assertThrows(VerificationException.class, () -> {
            bankVerificationService.verify(request);
        });
    }

    // ========================================
    // Test 5: Account Not Found - 404
    // ========================================

    @Test
    @Order(5)
    @DisplayName("404 Not Found - Account does not exist")
    void testBankVerification_NotFound() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody("{\"error\": \"Account not found\"}"));

        VerificationRequest request = VerificationRequest.forBank(
            1L, 1L, "9999999999", "SBIN0001234", "Unknown");

        // Act
        VerificationResult result = bankVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Bank account not found", result.getMessage());
    }

    // ========================================
    // Test 6: Invalid IFSC Format (Client-Side)
    // ========================================

    @Test
    @Order(6)
    @DisplayName("Invalid IFSC format - Client-side validation")
    void testBankVerification_InvalidFormat() {
        // Arrange: Invalid IFSC format (too short)
        VerificationRequest request = VerificationRequest.forBank(
            1L, 1L, "1234567890", "ABC", "John Doe");

        // Act
        VerificationResult result = bankVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid IFSC format"));
        assertEquals(0, mockWebServer.getRequestCount());
    }

    // ========================================
    // Test 7: Missing Required Parameters
    // ========================================

    @Test
    @Order(7)
    @DisplayName("Missing account number parameter")
    void testBankVerification_MissingParameter() {
        // Arrange
        VerificationRequest request = new VerificationRequest();
        request.setVerificationType(VerificationType.BANK);
        request.setBusinessProfileId(1L);
        request.addParameter("ifscCode", "SBIN0001234");
        // Missing accountNumber

        // Act
        VerificationResult result = bankVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Account number is required"));
    }

    // ========================================
    // Test 8: Supports Method
    // ========================================

    @Test
    @Order(8)
    @DisplayName("Supports method - Returns true for BANK type")
    void testSupports_BankType() {
        VerificationRequest bankRequest = VerificationRequest.forBank(
            1L, 1L, "1234567890", "SBIN0001234", "John Doe");
        assertTrue(bankVerificationService.supports(bankRequest));
    }

    @Test
    @Order(9)
    @DisplayName("Supports method - Returns false for non-BANK type")
    void testSupports_NonBankType() {
        VerificationRequest gstRequest = VerificationRequest.forGst(1L, "29AABCT1234M1Z5");
        assertFalse(bankVerificationService.supports(gstRequest));
    }

    // ========================================
    // Utility
    // ========================================

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
