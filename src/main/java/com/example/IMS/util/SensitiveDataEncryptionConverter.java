package com.example.IMS.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JPA Attribute Converter for encrypting sensitive data at rest
 * Used for PAN numbers, bank account numbers, and other PII
 * 
 * IMPORTANT: In production, replace with proper key management:
 * - Use AWS KMS, Azure Key Vault, or HashiCorp Vault
 * - Rotate keys periodically
 * - Use environment-specific encryption keys
 * - Never commit encryption keys to version control
 * 
 * This is a placeholder implementation for development.
 */
@Converter
public class SensitiveDataEncryptionConverter implements AttributeConverter<String, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataEncryptionConverter.class);
    
    // TODO: Move to secure key management service in production
    // This is a placeholder key - REPLACE IN PRODUCTION
    private static final String SECRET_KEY = System.getenv()
        .getOrDefault("ENCRYPTION_SECRET_KEY", "FlowTrack2026SecKey"); // Must be 16 chars for AES-128
    
    private static final String ALGORITHM = "AES";
    
    /**
     * Encrypt data before storing to database
     */
    @Override
    public String convertToDatabaseColumn(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.isEmpty()) {
            return sensitiveData;
        }
        
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8), 
                ALGORITHM
            );
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(sensitiveData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            logger.error("Error encrypting sensitive data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt data when reading from database
     */
    @Override
    public String convertToEntityAttribute(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8), 
                ALGORITHM
            );
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Error decrypting sensitive data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
