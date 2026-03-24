package com.example.IMS.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA Attribute Converter for encrypting sensitive data at rest using AES-GCM
 * Used for PAN numbers, bank account numbers, and other PII
 * 
 * SECURITY FEATURES:
 * - AES-GCM (Galois/Counter Mode) for authenticated encryption
 * - Random IV (Initialization Vector) for each encryption operation
 * - 128-bit authentication tag to prevent tampering
 * - Different ciphertext for same plaintext (non-deterministic)
 * 
 * STORAGE FORMAT: [IV (12 bytes)][Ciphertext + Auth Tag]
 * 
 * IMPORTANT: In production, replace with proper key management:
 * - Use AWS KMS, Azure Key Vault, or HashiCorp Vault
 * - Rotate keys periodically
 * - Use environment-specific encryption keys (minimum 32 bytes for AES-256)
 * - Never commit encryption keys to version control
 * 
 * This is a production-ready implementation but requires external key management.
 */
@Converter
public class SensitiveDataEncryptionConverter implements AttributeConverter<String, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataEncryptionConverter.class);
    
    // Algorithm: AES-GCM for authenticated encryption
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    
    // GCM parameters
    private static final int GCM_IV_LENGTH = 12; // 96 bits (recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag
    
    // SecureRandom for IV generation (thread-safe)
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // TODO: Move to secure key management service in production
    // This key should be 32 bytes (256 bits) for AES-256
    // Current fallback is for development only - REPLACE IN PRODUCTION
    private static final String SECRET_KEY = System.getenv()
        .getOrDefault("ENCRYPTION_SECRET_KEY", "FlowTrack2026-SecretKey-32Bytes!"); // Exactly 32 bytes for AES-256
    
    /**
     * Encrypt data before storing to database using AES-GCM
     * 
     * Process:
     * 1. Generate random 12-byte IV
     * 2. Encrypt plaintext with AES-GCM
     * 3. Prepend IV to ciphertext (for decryption)
     * 4. Base64 encode the result
     * 
     * Storage format: Base64([IV (12 bytes)][Ciphertext + Auth Tag])
     */
    @Override
    public String convertToDatabaseColumn(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.isEmpty()) {
            return sensitiveData;
        }
        
        try {
            // Generate random IV for this encryption operation
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Create GCM parameter spec with IV and tag length
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // Initialize cipher with AES-GCM
            SecretKeySpec secretKey = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8), 
                KEY_ALGORITHM
            );
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            
            // Encrypt the data
            byte[] encryptedBytes = cipher.doFinal(sensitiveData.getBytes(StandardCharsets.UTF_8));
            
            // Prepend IV to encrypted data
            byte[] result = new byte[GCM_IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, result, GCM_IV_LENGTH, encryptedBytes.length);
            
            // Base64 encode for database storage
            return Base64.getEncoder().encodeToString(result);
            
        } catch (Exception e) {
            logger.error("Error encrypting sensitive data with AES-GCM", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt data when reading from database using AES-GCM
     * 
     * Process:
     * 1. Base64 decode the stored value
     * 2. Extract IV from first 12 bytes
     * 3. Extract ciphertext from remaining bytes
     * 4. Decrypt with AES-GCM using extracted IV
     * 5. Verify authentication tag
     */
    @Override
    public String convertToEntityAttribute(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        try {
            // Base64 decode
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            
            // Validate minimum length (IV + at least some ciphertext + auth tag)
            if (decodedData.length < GCM_IV_LENGTH + 1) {
                throw new IllegalArgumentException("Invalid encrypted data format");
            }
            
            // Extract IV from first 12 bytes
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
            
            // Extract ciphertext (everything after IV)
            byte[] ciphertext = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // Create GCM parameter spec with extracted IV
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // Initialize cipher with AES-GCM
            SecretKeySpec secretKey = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8), 
                KEY_ALGORITHM
            );
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            // Decrypt and verify authentication tag
            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Error decrypting sensitive data with AES-GCM", e);
            throw new RuntimeException("Decryption failed - data may be tampered or corrupted", e);
        }
    }
}
