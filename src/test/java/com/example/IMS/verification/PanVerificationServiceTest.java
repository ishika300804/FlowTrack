/**
 * PAN Verification Service Test - Prompt 3
 * ========================================
 * 
 * Tests PanVerificationService in REAL mode using MockWebServer.
 * 
 * Scenarios:
 * - ✓ 200 OK: Successful verification
 * - ✓ 400 Bad Request: Invalid PAN format (no retry)
 * - ✓ 500 Internal Error: Server error (retry handled by WebClient)
 * - ✓ Timeout: Network delay exceeds configured timeout
 * 
 * PAN Format: 5 letters + 4 digits + 1 letter (e.g., AABCT1234M)
 */
package com.example.IMS.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.exception.VerificationException;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.service.verification.PanVerificationService;
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
    "verification.pan.api.key=test-pan-key-789",
    "verification.pan.timeout-ms=2000"
})
@DisplayName("PAN Verification Service Tests (Prompt 3)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PanVerificationServiceTest {

    @Autowired
    private PanVerificationService panVerificationService;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockServerUrl = mockWebServer.url("/pan/verify").toString();
        setField(panVerificationService, "panApiUrl", mockServerUrl);
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
    @DisplayName("200 OK - PAN verification successful")
    void testPanVerification_Success() throws InterruptedException {
        // Arrange
        String mockResponse = "{\n" +
            "    \"valid\": true,\n" +
            "    \"holderName\": \"JOHN DOE\",\n" +
            "    \"panNumber\": \"AABCT1234M\",\n" +
            "    \"status\": \"Active\"\n" +
            "}";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"));

        VerificationRequest request = VerificationRequest.forPan(1L, "AABCT1234M");

        // Act
        VerificationResult result = panVerificationService.verify(request);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("PAN verified successfully", result.getMessage());
        assertEquals("JOHN DOE", result.getData().get("holderName"));
        assertEquals("Active", result.getData().get("status"));

        // Verify HTTP request
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getBody().readUtf8().contains("AABCT1234M"));
    }

    // ========================================
    // Test 2: Invalid PAN - 400 Bad Request
    // ========================================

    @Test
    @Order(2)
    @DisplayName("400 Bad Request - Invalid PAN (no retry)")
    void testPanVerification_BadRequest() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\": \"Invalid PAN\"}"));

        VerificationRequest request = VerificationRequest.forPan(1L, "AABCT1234M");

        // Act
        VerificationResult result = panVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Invalid request to PAN API", result.getMessage());
        assertEquals(1, mockWebServer.getRequestCount());
    }

    // ========================================
    // Test 3: Server Error - 500
    // ========================================

    @Test
    @Order(3)
    @DisplayName("500 Internal Error - Triggers retry")
    void testPanVerification_ServerError() {
        // Arrange
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        VerificationRequest request = VerificationRequest.forPan(1L, "AABCT1234M");

        // Act & Assert
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            panVerificationService.verify(request);
        });

        assertEquals(VerificationType.PAN, exception.getVerificationType());
        assertTrue(mockWebServer.getRequestCount() > 0);
    }

    // ========================================
    // Test 4: Timeout
    // ========================================

    @Test
    @Order(4)
    @DisplayName("Timeout - Request exceeds configured timeout")
    void testPanVerification_Timeout() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"valid\": true}")
            .setBodyDelay(3, TimeUnit.SECONDS));

        VerificationRequest request = VerificationRequest.forPan(1L, "AABCT1234M");

        // Act & Assert
        assertThrows(VerificationException.class, () -> {
            panVerificationService.verify(request);
        });
    }

    // ========================================
    // Test 5: PAN Not Found - 404
    // ========================================

    @Test
    @Order(5)
    @DisplayName("404 Not Found - PAN does not exist")
    void testPanVerification_NotFound() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody("{\"error\": \"PAN not found\"}"));

        VerificationRequest request = VerificationRequest.forPan(1L, "XXXXX9999X");

        // Act
        VerificationResult result = panVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("PAN not found", result.getMessage());
    }

    // ========================================
    // Test 6: Invalid PAN Format (Client-Side)
    // ========================================

    @Test
    @Order(6)
    @DisplayName("Invalid PAN format - Client-side validation")
    void testPanVerification_InvalidFormat() {
        // Arrange: Invalid format (too short)
        VerificationRequest request = VerificationRequest.forPan(1L, "ABC");

        // Act
        VerificationResult result = panVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid PAN format"));
        assertEquals(0, mockWebServer.getRequestCount());
    }

    // ========================================
    // Test 7: Missing PAN Parameter
    // ========================================

    @Test
    @Order(7)
    @DisplayName("Missing PAN parameter")
    void testPanVerification_MissingParameter() {
        // Arrange
        VerificationRequest request = new VerificationRequest();
        request.setVerificationType(VerificationType.PAN);
        request.setBusinessProfileId(1L);

        // Act
        VerificationResult result = panVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("PAN is required", result.getMessage());
    }

    // ========================================
    // Test 8: Supports Method
    // ========================================

    @Test
    @Order(8)
    @DisplayName("Supports method - Returns true for PAN type")
    void testSupports_PanType() {
        VerificationRequest panRequest = VerificationRequest.forPan(1L, "AABCT1234M");
        assertTrue(panVerificationService.supports(panRequest));
    }

    @Test
    @Order(9)
    @DisplayName("Supports method - Returns false for non-PAN type")
    void testSupports_NonPanType() {
        VerificationRequest gstRequest = VerificationRequest.forGst(1L, "29AABCT1234M1Z5");
        assertFalse(panVerificationService.supports(gstRequest));
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
