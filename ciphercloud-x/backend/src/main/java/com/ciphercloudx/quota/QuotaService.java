package com.ciphercloudx.quota;

import com.ciphercloudx.dto.QuotaUpdateRequestDto;
import com.ciphercloudx.dto.UserResponseDto;
import com.ciphercloudx.entity.User;
import com.ciphercloudx.exception.AuthenticationException;
import com.ciphercloudx.repository.UserRepository;
import com.ciphercloudx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class QuotaService {

    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public QuotaService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public UserResponseDto updateUserQuota(QuotaUpdateRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Validate that new quota is not less than current usage
        if (request.getStorageQuota() < user.getUsedStorage()) {
            throw new IllegalArgumentException("New quota cannot be less than current usage");
        }

        user.setStorageQuota(request.getStorageQuota());
        userRepository.save(user);

        log.info("Updated quota for user {}: {} bytes", user.getUsername(), request.getStorageQuota());

        return userService.getUserById(user.getId());
    }

    @Transactional(readOnly = true)
    public long getUserQuota(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        return user.getStorageQuota();
    }

    @Transactional(readOnly = true)
    public long getUserUsedStorage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        return user.getUsedStorage();
    }

    @Transactional(readOnly = true)
    public double getUserStorageUsagePercentage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        
        if (user.getStorageQuota() == 0) {
            return 0;
        }
        
        return (double) user.getUsedStorage() / user.getStorageQuota() * 100;
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableStorage(Long userId, long requestedSize) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        return user.hasAvailableStorage(requestedSize);
    }

    @Transactional
    public void recalculateUserStorage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // This would recalculate based on actual file sizes
        // For now, we trust the stored value
        log.info("Recalculated storage for user {}: {} bytes", user.getUsername(), user.getUsedStorage());
    }
}
