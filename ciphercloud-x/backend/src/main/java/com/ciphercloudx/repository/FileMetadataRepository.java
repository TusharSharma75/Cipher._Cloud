package com.ciphercloudx.repository;

import com.ciphercloudx.entity.FileMetadata;
import com.ciphercloudx.enums.IntegrityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    Optional<FileMetadata> findByStoredFilename(String storedFilename);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.owner.id = :ownerId AND f.isDeleted = false")
    List<FileMetadata> findByOwnerIdAndNotDeleted(@Param("ownerId") Long ownerId);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.owner.id = :ownerId AND f.isDeleted = false")
    Page<FileMetadata> findByOwnerIdAndNotDeleted(@Param("ownerId") Long ownerId, Pageable pageable);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.owner.id = :ownerId AND f.folderPath = :folderPath AND f.isDeleted = false")
    List<FileMetadata> findByOwnerIdAndFolderPath(@Param("ownerId") Long ownerId, @Param("folderPath") String folderPath);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.parentFileId = :parentFileId ORDER BY f.versionNumber DESC")
    List<FileMetadata> findVersionsByParentFileId(@Param("parentFileId") Long parentFileId);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.parentFileId = :parentFileId AND f.isLatest = true")
    Optional<FileMetadata> findLatestVersion(@Param("parentFileId") Long parentFileId);
    
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.owner.id = :ownerId AND f.isDeleted = false")
    Long countByOwnerId(@Param("ownerId") Long ownerId);
    
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.owner.id = :ownerId AND f.isDeleted = false AND f.uploadTimestamp >= :since")
    Long countByOwnerIdSince(@Param("ownerId") Long ownerId, @Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(f.fileSize) FROM FileMetadata f WHERE f.owner.id = :ownerId AND f.isDeleted = false")
    Long sumFileSizeByOwnerId(@Param("ownerId") Long ownerId);
    
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.isDeleted = false")
    Long countAllActiveFiles();
    
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.isDeleted = true")
    Long countAllDeletedFiles();
    
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.uploadTimestamp >= :since")
    Long countUploadsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(f.fileSize) FROM FileMetadata f WHERE f.isDeleted = false")
    Long sumTotalFileSize();
    
    @Query("SELECT f.contentType, COUNT(f), SUM(f.fileSize) FROM FileMetadata f WHERE f.isDeleted = false GROUP BY f.contentType")
    List<Object[]> getFileTypeDistribution();
    
    @Modifying
    @Query("UPDATE FileMetadata f SET f.isLatest = false WHERE f.parentFileId = :parentFileId")
    void clearLatestVersionFlag(@Param("parentFileId") Long parentFileId);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.integrityStatus = :status")
    List<FileMetadata> findByIntegrityStatus(@Param("status") IntegrityStatus status);
    
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.integrityStatus = :status")
    Long countByIntegrityStatus(@Param("status") IntegrityStatus status);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.isDeleted = false AND f.originalFilename LIKE %:searchTerm%")
    List<FileMetadata> searchByFilename(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.owner.id = :ownerId AND f.isDeleted = false AND f.originalFilename LIKE %:searchTerm%")
    List<FileMetadata> searchByOwnerAndFilename(@Param("ownerId") Long ownerId, @Param("searchTerm") String searchTerm);
}
