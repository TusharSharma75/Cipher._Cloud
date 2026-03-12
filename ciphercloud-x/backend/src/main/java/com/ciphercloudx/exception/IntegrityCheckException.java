package com.ciphercloudx.exception;

public class IntegrityCheckException extends RuntimeException {
    
    private final String expectedHash;
    private final String actualHash;
    
    public IntegrityCheckException(String message, String expectedHash, String actualHash) {
        super(message);
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
    }
    
    public String getExpectedHash() {
        return expectedHash;
    }
    
    public String getActualHash() {
        return actualHash;
    }
}
