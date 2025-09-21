package com.ecommerce.cache.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String name;
    private String description;
    private String category;
    private String brand;
    private String sku;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stockQuantity;
    private Double rating;
    private Integer reviewCount;
    private List<String> images;
    private Map<String, String> attributes;
    private List<String> tags;
    private boolean active;
    private boolean featured;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // SEO fields
    private String metaTitle;
    private String metaDescription;
    private String slug;
}