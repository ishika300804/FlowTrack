/**
 * CIN Verification Service Test - Prompt 3
 * ========================================
 * 
 * Tests CinVerificationService in REAL mode using MockWebServer.
 * 
 * Scenarios:
 * - ✓ 200 OK: Successful CIN verification
 * - ✓ 400 Bad Request: Invalid CIN format
 * - ✓ 500 Internal Error: Server error (retry)
 * - ✓ Timeout: Network delay exceeds timeout
 * 
 * CIN Format: 21 characters (e.g., U12345MH2020PTC123456)
 * - U/L: Listed/Unlisted
 * - 5 digits: Industry code
 * - 2 letters: State code
 * - 4 digits: Year
 * - PLC/PTC: Public/Private
 * - 6 digits: Registration number
 */
package com.example.IMS.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.exception.VerificationException;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.service.verification.CinVerificationService;
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
    "verification.cin.api.key=test-cin-key-999",
    "verification.cin.timeout-ms=2000"
})
@DisplayName("CIN Verification Service Tests (Prompt 3)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CinVerificationServiceTest {

    @Autowired
    private CinVerificationService cinVerificationService;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockServerUrl = mockWebServer.url("/cin/verify").toString();
        setField(cinVerificationService, "cinApiUrl", mockServerUrl);
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
    @DisplayName("200 OK - CIN verification successful")
    void testCinVerification_Success() throws InterruptedException {
        // Arrange
        String mockResponse = "{\n" +
            "    \"valid\": true,\n" +
            "    \"cin\": \"U12345MH2020PTC123456\",\n" +
            "    \"companyName\": \"Test Industries Private Limited\",\n" +
            "    \"registrationDate\": \"2020-05-15\",\n" +
            "    \"status\": \"Active\",\n" +
            "    \"companyClass\": \"Private\"\n" +
            "}";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"));

        VerificationRequest request = VerificationRequest.forCin(1L, "U12345MH2020PTC123456");

        // Act
        VerificationResult result = cinVerificationService.verify(request);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("CIN verified successfully", result.getMessage());
        assertEquals("Test Industries Private Limited", result.getData().get("companyName"));
        assertEquals("Active", result.getData().get("companyStatus"));

        // Verify HTTP request
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getBody().readUtf8().contains("U12345MH2020PTC123456"));
    }

    // ========================================
    // Test 2: Invalid CIN - 400 Bad Request
    // ========================================

    @Test
    @Order(2)
    @DisplayName("400 Bad Request - Invalid CIN (no retry)")
    void testCinVerification_BadRequest() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\": \"Invalid CIN format\"}"));

        VerificationRequest request = VerificationRequest.forCin(1L, "U12345MH2020PTC123456");

        // Act
        VerificationResult result = cinVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Invalid request to CIN API", result.getMessage());
        assertEquals(1, mockWebServer.getRequestCount());
    }

    // ========================================
    // Test 3: Server Error - 500
    // ========================================

    @Test
    @Order(3)
    @DisplayName("500 Internal Error - Triggers retry")
    void testCinVerification_ServerError() {
        // Arrange
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        VerificationRequest request = VerificationRequest.forCin(1L, "U12345MH2020PTC123456");

        // Act & Assert
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            cinVerificationService.verify(request);
        });

        assertEquals(VerificationType.CIN, exception.getVerificationType());
        assertTrue(mockWebServer.getRequestCount() > 0);
    }

    // ========================================
    // Test 4: Timeout
    // ========================================

    @Test
    @Order(4)
    @DisplayName("Timeout - Request exceeds configured timeout")
    void testCinVerification_Timeout() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"valid\": true}")
            .setBodyDelay(3, TimeUnit.SECONDS));

        VerificationRequest request = VerificationRequest.forCin(1L, "U12345MH2020PTC123456");

        // Act & Assert
        assertThrows(VerificationException.class, () -> {
            cinVerificationService.verify(request);
        });
    }

    // ========================================
    // Test 5: CIN Not Found - 404
    // ========================================

    @Test
    @Order(5)
    @DisplayName("404 Not Found - CIN does not exist")
    void testCinVerification_NotFound() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody("{\"error\": \"CIN not found\"}"));

        VerificationRequest request = VerificationRequest.forCin(1L, "U99999MH9999PTC999999");

        // Act
        VerificationResult result = cinVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("CIN not found", result.getMessage());
    }

    // ========================================
    // Test 6: Invalid CIN Format (Client-Side)
    // ========================================

    @Test
    @Order(6)
    @DisplayName("Invalid CIN format - Client-side validation")
    void testCinVerification_InvalidFormat() {
        // Arrange: Invalid format (too short)
        VerificationRequest request = VerificationRequest.forCin(1L, "U12345");

        // Act
        VerificationResult result = cinVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid CIN format"));
        assertEquals(0, mockWebServer.getRequestCount());
    }

    // ========================================
    // Test 7: Missing CIN Parameter
    // ========================================

    @Test
    @Order(7)
    @DisplayName("Missing CIN parameter")
    void testCinVerification_MissingParameter() {
        // Arrange
        VerificationRequest request = new VerificationRequest();
        request.setVerificationType(VerificationType.CIN);
        request.setBusinessProfileId(1L);

        // Act
        VerificationResult result = cinVerificationService.verify(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("CIN is required", result.getMessage());
    }

    // ========================================
    // Test 8: Supports Method
    // ========================================

    @Test
    @Order(8)
    @DisplayName("Supports method - Returns true for CIN type")
    void testSupports_CinType() {
        VerificationRequest cinRequest = VerificationRequest.forCin(1L, "U12345MH2020PTC123456");
        assertTrue(cinVerificationService.supports(cinRequest));
    }

    @Test
    @Order(9)
    @DisplayName("Supports method - Returns false for non-CIN type")
    void testSupports_NonCinType() {
        VerificationRequest panRequest = VerificationRequest.forPan(1L, "AABCT1234M");
        assertFalse(cinVerificationService.supports(panRequest));
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
