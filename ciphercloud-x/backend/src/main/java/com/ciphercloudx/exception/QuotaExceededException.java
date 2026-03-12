package com.ciphercloudx.exception;

public class QuotaExceededException extends RuntimeException {
    
    private final long currentUsage;
    private final long quota;
    private final long requestedSize;
    
    public QuotaExceededException(String message, long currentUsage, long quota, long requestedSize) {
        super(message);
        this.currentUsage = currentUsage;
        this.quota = quota;
        this.requestedSize = requestedSize;
    }
    
    public long getCurrentUsage() {
        return currentUsage;
    }
    
    public long getQuota() {
        return quota;
    }
    
    public long getRequestedSize() {
        return requestedSize;
    }
}
