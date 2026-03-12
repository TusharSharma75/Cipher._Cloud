package com.ciphercloudx.storage;

import java.io.InputStream;

public interface StorageProvider {
    
    /**
     * Upload a file to the storage provider
     * 
     * @param key Unique identifier for the file
     * @param inputStream Input stream containing the file data
     * @param contentType MIME type of the file
     * @param contentLength Size of the file in bytes
     * @return true if upload was successful
     */
    boolean upload(String key, InputStream inputStream, String contentType, long contentLength);
    
    /**
     * Download a file from the storage provider
     * 
     * @param key Unique identifier for the file
     * @return Input stream containing the file data, or null if not found
     */
    InputStream download(String key);
    
    /**
     * Delete a file from the storage provider
     * 
     * @param key Unique identifier for the file
     * @return true if deletion was successful or file didn't exist
     */
    boolean delete(String key);
    
    /**
     * Check if a file exists in the storage provider
     * 
     * @param key Unique identifier for the file
     * @return true if the file exists
     */
    boolean exists(String key);
    
    /**
     * Get the size of a file
     * 
     * @param key Unique identifier for the file
     * @return Size in bytes, or -1 if file not found
     */
    long getSize(String key);
    
    /**
     * Get the URL for accessing the file (if applicable)
     * 
     * @param key Unique identifier for the file
     * @return URL string, or null if not supported
     */
    String getUrl(String key);
    
    /**
     * Get the provider type
     * 
     * @return Storage provider type identifier
     */
    String getProviderType();
    
    /**
     * Check if the storage provider is available/healthy
     * 
     * @return true if the provider is available
     */
    boolean isHealthy();
    
    /**
     * Get the name of the storage provider
     * 
     * @return Provider name
     */
    String getName();
}
