package com.ecommerce.cache.service;

import com.ecommerce.cache.cache.RedisCacheService;
import com.ecommerce.cache.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final RedisCacheService redisCacheService;

    public Map<String, Object> createUserSession(String userId) {
        String sessionId = UUID.randomUUID().toString();

        User user = getUserFromDatabase(userId);
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", sessionId);
        sessionData.put("userId", userId);
        sessionData.put("email", user.getEmail());
        sessionData.put("firstName", user.getFirstName());
        sessionData.put("lastName", user.getLastName());
        sessionData.put("role", user.getRole());
        sessionData.put("preferences", user.getPreferences());
        sessionData.put("loginTime", LocalDateTime.now().toString());
        sessionData.put("lastActivity", LocalDateTime.now().toString());
        sessionData.put("isAuthenticated", true);

        // Store in L2 cache using write-through pattern
        redisCacheService.setUserSession(sessionId, sessionData);

        log.info("Created session for user: {} sessionId: {}", userId, sessionId);
        return sessionData;
    }

    public Map<String, Object> getUserSession(String sessionId) {
        Map<String, Object> session = redisCacheService.getUserSession(sessionId);

        if (session != null) {
            // Update last activity
            redisCacheService.updateSessionField(sessionId, "lastActivity", LocalDateTime.now().toString());
        }

        return session;
    }

    public void updateSessionPreferences(String sessionId, Map<String, Object> preferences) {
        redisCacheService.updateSessionField(sessionId, "preferences", preferences);
        log.debug("Updated session preferences for session: {}", sessionId);
    }

    public void invalidateUserSession(String sessionId) {
        redisCacheService.invalidateUserSession(sessionId);
        log.info("Invalidated session: {}", sessionId);
    }

    private User getUserFromDatabase(String userId) {
        // Simulate database fetch
        try {
            Thread.sleep(20); // Simulate DB latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return User.builder()
                .id(userId)
                .email("user" + userId + "@example.com")
                .firstName("User")
                .lastName(userId)
                .role("USER")
                .status("ACTIVE")
                .preferences(Map.of("currency", "USD", "language", "en"))
                .createdAt(LocalDateTime.now().minusDays(30))
                .lastLoginAt(LocalDateTime.now())
                .build();
    }
}