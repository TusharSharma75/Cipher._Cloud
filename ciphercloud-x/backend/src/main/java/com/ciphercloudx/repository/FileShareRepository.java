package com.ciphercloudx.repository;

import com.ciphercloudx.entity.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    
    Optional<FileShare> findByShareToken(String shareToken);
    
    @Query("SELECT fs FROM FileShare fs WHERE fs.file.id = :fileId AND fs.isActive = true")
    List<FileShare> findActiveSharesByFileId(@Param("fileId") Long fileId);
    
    @Query("SELECT fs FROM FileShare fs WHERE fs.sharedWith.id = :userId AND fs.isActive = true")
    List<FileShare> findSharesBySharedWithUserId(@Param("userId") Long userId);
    
    @Query("SELECT fs FROM FileShare fs WHERE fs.sharedBy.id = :userId")
    List<FileShare> findSharesBySharedByUserId(@Param("userId") Long userId);
    
    @Query("SELECT fs FROM FileShare fs WHERE fs.file.id = :fileId AND fs.sharedWith.id = :userId AND fs.isActive = true")
    Optional<FileShare> findShareByFileAndUser(@Param("fileId") Long fileId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(fs) FROM FileShare fs WHERE fs.sharedBy.id = :userId AND fs.isActive = true")
    Long countActiveSharesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT fs FROM FileShare fs WHERE fs.isActive = true AND fs.expiryDate < :now")
    List<FileShare> findExpiredShares(@Param("now") LocalDateTime now);
    
    @Query("SELECT fs FROM FileShare fs WHERE fs.isActive = true AND fs.maxDownloads IS NOT NULL AND fs.downloadCount >= fs.maxDownloads")
    List<FileShare> findSharesWithReachedDownloadLimit();
    
    boolean existsByShareToken(String shareToken);
}
