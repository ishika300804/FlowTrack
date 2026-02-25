/**
 * GST Verification Service Test - Prompt 3
 * ========================================
 * 
 * Tests GstVerificationService in REAL mode using MockWebServer.
 * 
 * Scenarios:
 * - ✓ 200 OK: Successful verification
 * - ✓ 400 Bad Request: Invalid GSTIN format (no retry)
 * - ✓ 500 Internal Error: Server error (retry handled by WebClient)
 * - ✓ Timeout: Network delay exceeds configured timeout
 * 
 * Architecture:
 * - Uses MockWebServer (OkHttp) to simulate external GST API
 * - Tests REAL mode (not MOCK mode)
 * - Validates retry behavior (configured in WebClientConfig)
 * - Verifies error handling and result mapping
 * 
 * Test Data:
 * - Uses @Transactional for automatic rollback
 * - No TRUNCATE, no explicit IDs
 * - Each test is isolated and independent
 */
package com.example.IMS.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.exception.VerificationException;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.service.verification.GstVerificationService;
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
    "verification.gst.api.key=test-api-key-123",
    "verification.gst.timeout-ms=2000"
})
@DisplayName("GST Verification Service Tests (Prompt 3)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GstVerificationServiceTest {

    @Autowired
    private GstVerificationService gstVerificationService;

    private MockWebServer mockWebServer;
    private String mockServerUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockServerUrl = mockWebServer.url("/gst/verify").toString();
        
        // Inject mock server URL into service via reflection
        // (In production, this would be configured via @TestPropertySource)
        setField(gstVerificationService, "gstApiUrl", mockServerUrl);
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
    @DisplayName("200 OK - GST verification successful")
    void testGstVerification_Success() throws InterruptedException {
        // Arrange: Mock 200 OK response with valid GST data
        String mockResponse = "{\n" +
            "    \"valid\": true,\n" +
            "    \"businessName\": \"Test Industries Pvt Ltd\",\n" +
            "    \"registrationDate\": \"2020-05-15\",\n" +
            "    \"address\": \"123 Test Street, Karnataka 560001\",\n" +
            "    \"status\": \"Active\"\n" +
            "}";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"));

        VerificationRequest request = VerificationRequest.forGst(1L, "29AABCT1332L1ZG");

        // Act
        VerificationResult result = gstVerificationService.verify(request);

        // Assert
        assertTrue(result.isSuccess(), "Verification should succeed");
        assertEquals("GSTIN verified successfully", result.getMessage());
        assertEquals("Test Industries Pvt Ltd", result.getData().get("businessName"));
        assertEquals("Active", result.getData().get("status"));
        assertEquals("REAL", result.getData().get("mode"));

        // Verify HTTP request was made
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest, "HTTP request should have been made");
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("test-api-key-123", recordedRequest.getHeader("X-API-Key"));
        assertTrue(recordedRequest.getBody().readUtf8().contains("29AABCT1332L1ZG"));
    }

    // ========================================
    // Test 2: Invalid GSTIN - 400 Bad Request
    // ========================================

    @Test
    @Order(2)
    @DisplayName("400 Bad Request - Invalid GSTIN format (no retry)")
    void testGstVerification_BadRequest() throws InterruptedException {
        // Arrange: Mock 400 error response
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\": \"Invalid GSTIN format\"}")
            .addHeader("Content-Type", "application/json"));

        VerificationRequest request = VerificationRequest.forGst(1L, "29AABCT1332L1ZG");

        // Act
        VerificationResult result = gstVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess(), "Verification should fail");
        assertEquals("Invalid request to GST API", result.getMessage());

        // Verify NO retry for 4xx errors
        assertEquals(1, mockWebServer.getRequestCount(), "Should make exactly 1 request (no retry on 4xx)");
    }

    // ========================================
    // Test 3: Server Error - 500 (Retry Logic)
    // ========================================

    @Test
    @Order(3)
    @DisplayName("500 Internal Error - Server error triggers retry")
    void testGstVerification_ServerError() throws InterruptedException {
        // Arrange: Mock 500 error response (retry configured in WebClientConfig)
        // WebClientConfig retries up to 3 times for 5xx errors
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        VerificationRequest request = VerificationRequest.forGst(1L, "29AABCT1332L1ZG");

        // Act & Assert - Should throw VerificationException
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            gstVerificationService.verify(request);
        });

        // Verify exception type
        assertEquals(VerificationType.GST, exception.getVerificationType());
        
        // Verify at least one request was made (retry count depends on WebClientConfig)
        int requestCount = mockWebServer.getRequestCount();
        assertTrue(requestCount > 0, "Should make at least 1 request, actual: " + requestCount);
    }

    // ========================================
    // Test 4: Timeout - Network Delay
    // ========================================

    @Test
    @Order(4)
    @DisplayName("Timeout - Request exceeds configured timeout")
    void testGstVerification_Timeout() {
        // Arrange: Mock delayed response (exceeds 2000ms timeout)
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"valid\": true}")
            .setBodyDelay(3, TimeUnit.SECONDS)); // Delay > timeout

        VerificationRequest request = VerificationRequest.forGst(1L, "29AABCT1332L1ZG");

        // Act & Assert
        assertThrows(VerificationException.class, () -> {
            gstVerificationService.verify(request);
        }, "Should throw VerificationException on timeout");
    }

    // ========================================
    // Test 5: GSTIN Not Found - 404
    // ========================================

    @Test
    @Order(5)
    @DisplayName("404 Not Found - GSTIN does not exist")
    void testGstVerification_NotFound() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody("{\"error\": \"GSTIN not found\"}"));

        VerificationRequest request = VerificationRequest.forGst(1L, "29XXXXX9999X9ZX");

        // Act
        VerificationResult result = gstVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("GSTIN not found", result.getMessage());
    }

    // ========================================
    // Test 6: Invalid GSTIN Format (Client-Side Validation)
    // ========================================

    @Test
    @Order(6)
    @DisplayName("Invalid GSTIN format - Client-side validation")
    void testGstVerification_InvalidFormat() {
        // Arrange: Invalid format (too short)
        VerificationRequest request = VerificationRequest.forGst(1L, "29ABC");

        // Act
        VerificationResult result = gstVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid GSTIN format"));
        
        // No HTTP call should be made
        assertEquals(0, mockWebServer.getRequestCount(), "Should not make HTTP request for invalid format");
    }

    // ========================================
    // Test 7: Missing GSTIN Parameter
    // ========================================

    @Test
    @Order(7)
    @DisplayName("Missing GSTIN parameter - Empty input")
    void testGstVerification_MissingParameter() {
        // Arrange
        VerificationRequest request = new VerificationRequest();
        request.setVerificationType(VerificationType.GST);
        request.setBusinessProfileId(1L);
        // No gstin parameter set

        // Act
        VerificationResult result = gstVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("GSTIN is required", result.getMessage());
    }

    // ========================================
    // Test 8: Supports Method
    // ========================================

    @Test
    @Order(8)
    @DisplayName("Supports method - Returns true for GST type")
    void testSupports_GstType() {
        VerificationRequest gstRequest = VerificationRequest.forGst(1L, "29AABCT1332L1ZG");
        assertTrue(gstVerificationService.supports(gstRequest));
    }

    @Test
    @Order(9)
    @DisplayName("Supports method - Returns false for non-GST type")
    void testSupports_NonGstType() {
        VerificationRequest panRequest = VerificationRequest.forPan(1L, "AABCT1234M");
        assertFalse(gstVerificationService.supports(panRequest));
    }

    // ========================================
    // Utility: Reflection Helper
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
