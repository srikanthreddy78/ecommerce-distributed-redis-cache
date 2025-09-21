package com.ecommerce.cache.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String status; // ACTIVE, INACTIVE, SUSPENDED
    private String role;   // USER, ADMIN, PREMIUM

    // Preferences
    private Map<String, Object> preferences;
    private List<String> favoriteCategories;
    private String preferredCurrency;
    private String language;

    // Tracking
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Address information
    private Address defaultShippingAddress;
    private Address defaultBillingAddress;
}