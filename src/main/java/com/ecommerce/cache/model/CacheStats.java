package com.ecommerce.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStats {
    private long hits;
    private long misses;
    private long sets;
    private long deletes;
    private long totalRequests;
    private String hitRate;

    // Tier-specific stats
    private TierStats l1ProductStats;
    private TierStats l2SessionStats;
    private TierStats l3InventoryStats;
    private TierStats l4AnalyticsStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierStats {
        private String tierName;
        private long hits;
        private long misses;
        private long sets;
        private String hitRate;
        private long memoryUsage; // in bytes
        private int keyCount;
    }
}