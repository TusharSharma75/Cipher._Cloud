package com.ciphercloudx.encryption;

import com.ciphercloudx.exception.EncryptionException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

@Service
@Slf4j
public class EncryptionService {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_PADDING = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int RSA_KEY_SIZE = 2048;

    @Value("${encryption.rsa.private-key:}")
    private String rsaPrivateKeyBase64;

    @Value("${encryption.rsa.public-key:}")
    private String rsaPublicKeyBase64;

    @Value("${encryption.master-password:}")
    private String masterPassword;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        try {
            if (rsaPrivateKeyBase64.isEmpty() || rsaPublicKeyBase64.isEmpty()) {
                log.info("Generating new RSA key pair...");
                generateAndStoreKeyPair();
            } else {
                log.info("Loading RSA keys from configuration...");
                loadKeysFromConfig();
            }
            log.info("Encryption service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize encryption service", e);
            throw new EncryptionException("Failed to initialize encryption service", e);
        }
    }

    private void generateAndStoreKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_SIZE, secureRandom);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        
        // Log the keys so they can be saved for future use
        String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        
        log.warn("=================================================================");
        log.warn("NEW RSA KEY PAIR GENERATED. SAVE THESE KEYS FOR PRODUCTION USE:");
        log.warn("Public Key: {}", publicKeyStr);
        log.warn("Private Key: {}", privateKeyStr);
        log.warn("=================================================================");
    }

    private void loadKeysFromConfig() throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicKeyBytes = Base64.getDecoder().decode(rsaPublicKeyBase64);
        byte[] privateKeyBytes = Base64.getDecoder().decode(rsaPrivateKeyBase64);
        
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        this.publicKey = keyFactory.generatePublic(publicKeySpec);
        
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        this.privateKey = keyFactory.generatePrivate(privateKeySpec);
    }

    /**
     * Generate a random AES-256 key for file encryption
     */
    public SecretKey generateAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_SIZE, secureRandom);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to generate AES key", e);
        }
    }

    /**
     * Generate a random IV for GCM mode
     */
    public byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypt file data using AES-256 GCM with a random key
     * Returns the encrypted data and the encrypted AES key
     */
    public EncryptionResult encryptFile(byte[] fileData) {
        try {
            // Generate random AES key
            SecretKey aesKey = generateAesKey();
            byte[] iv = generateIv();
            
            // Encrypt file with AES-GCM
            Cipher aesCipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParameterSpec);
            byte[] encryptedData = aesCipher.doFinal(fileData);
            
            // Encrypt AES key with RSA
            Cipher rsaCipher = Cipher.getInstance(RSA_PADDING);
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());
            
            return EncryptionResult.builder()
                    .encryptedData(encryptedData)
                    .encryptedKey(Base64.getEncoder().encodeToString(encryptedAesKey))
                    .iv(Base64.getEncoder().encodeToString(iv))
                    .build();
                    
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt file", e);
        }
    }

    /**
     * Decrypt file data using the encrypted AES key
     */
    public byte[] decryptFile(byte[] encryptedData, String encryptedAesKeyBase64, String ivBase64) {
        try {
            // Decrypt AES key with RSA
            byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedAesKeyBase64);
            Cipher rsaCipher = Cipher.getInstance(RSA_PADDING);
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
            
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            
            // Decrypt file with AES-GCM
            Cipher aesCipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmParameterSpec);
            
            return aesCipher.doFinal(encryptedData);
            
        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt file", e);
        }
    }

    /**
     * Stream-based file encryption for large files
     */
    public StreamingEncryptionResult encryptFileStream(InputStream inputStream, OutputStream outputStream) {
        try {
            SecretKey aesKey = generateAesKey();
            byte[] iv = generateIv();
            
            Cipher aesCipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParameterSpec);
            
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, aesCipher);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
            }
            cipherOutputStream.flush();
            cipherOutputStream.close();
            
            // Encrypt AES key with RSA
            Cipher rsaCipher = Cipher.getInstance(RSA_PADDING);
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());
            
            return StreamingEncryptionResult.builder()
                    .encryptedKey(Base64.getEncoder().encodeToString(encryptedAesKey))
                    .iv(Base64.getEncoder().encodeToString(iv))
                    .build();
                    
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt file stream", e);
        }
    }

    /**
     * Stream-based file decryption for large files
     */
    public void decryptFileStream(InputStream inputStream, OutputStream outputStream, 
                                   String encryptedAesKeyBase64, String ivBase64) {
        try {
            // Decrypt AES key with RSA
            byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedAesKeyBase64);
            Cipher rsaCipher = Cipher.getInstance(RSA_PADDING);
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
            
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            
            Cipher aesCipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmParameterSpec);
            
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, aesCipher);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            cipherInputStream.close();
            
        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt file stream", e);
        }
    }

    /**
     * Calculate SHA-256 hash of data
     */
    public String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to calculate hash", e);
        }
    }

    /**
     * Calculate SHA-256 hash from input stream
     */
    public String calculateHash(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            throw new EncryptionException("Failed to calculate hash from stream", e);
        }
    }

    /**
     * Verify data integrity using SHA-256 hash
     */
    public boolean verifyIntegrity(byte[] data, String expectedHash) {
        String actualHash = calculateHash(data);
        return actualHash.equalsIgnoreCase(expectedHash);
    }

    /**
     * Hash password using BCrypt
     */
    public String hashPassword(String password) {
        return org.springframework.security.crypto.bcrypt.BCrypt.hashpw(password, 
                org.springframework.security.crypto.bcrypt.BCrypt.gensalt(12));
    }

    /**
     * Verify password against hash
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(password, hashedPassword);
    }

    /**
     * Generate secure random token
     */
    public String generateSecureToken(int length) {
        byte[] token = new byte[length];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    /**
     * Generate OTP secret for 2FA
     */
    public String generateOtpSecret() {
        return generateSecureToken(20);
    }

    /**
     * Generate backup codes for 2FA recovery
     */
    public String[] generateBackupCodes(int count) {
        String[] codes = new String[count];
        for (int i = 0; i < count; i++) {
            codes[i] = generateNumericCode(8);
        }
        return codes;
    }

    /**
     * Generate numeric code
     */
    public String generateNumericCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @lombok.Builder
    @lombok.Data
    public static class EncryptionResult {
        private byte[] encryptedData;
        private String encryptedKey;
        private String iv;
    }

    @lombok.Builder
    @lombok.Data
    public static class StreamingEncryptionResult {
        private String encryptedKey;
        private String iv;
    }
}
