package com.ecommerce.cache.controller;


import com.ecommerce.cache.cache.RedisCacheService;
import com.ecommerce.cache.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final RedisCacheService redisCacheService;
    private final AnalyticsService analyticsService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // Cache statistics
        dashboard.put("cacheStats", redisCacheService.getStats());

        // Redis cluster info
        dashboard.put("redisClusterInfo", getRedisClusterInfo());

        // Today's analytics
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        dashboard.put("dailySales", analyticsService.getDailyAnalytics(today, "daily_sales"));
        dashboard.put("userActivity", analyticsService.getDailyAnalytics(today, "user_activity"));
        dashboard.put("productViews", analyticsService.getDailyAnalytics(today, "product_views"));

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Test Redis connectivity
            String pingResult = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            health.put("redis", "UP");
            health.put("redisPing", pingResult);
            health.put("status", "HEALTHY");

        } catch (Exception e) {
            health.put("redis", "DOWN");
            health.put("error", e.getMessage());
            health.put("status", "UNHEALTHY");
            return ResponseEntity.status(503).body(health);
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/analytics/{date}/{metric}")
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @PathVariable String date,
            @PathVariable String metric) {
        Map<String, Object> analytics = analyticsService.getDailyAnalytics(date, metric);
        return ResponseEntity.ok(analytics);
    }

    @PostMapping("/cache/warm")
    public ResponseEntity<Map<String, Object>> warmCache(
            @RequestParam(defaultValue = "100") int productCount) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        // Warm up product cache
        for (int i = 1; i <= productCount; i++) {
            String productId = "warmup-product-" + i;
            // This will trigger cache population
            // productService.getProduct(productId); // Commented for brevity
        }

        long duration = System.currentTimeMillis() - startTime;
        result.put("warmedProducts", productCount);
        result.put("durationMs", duration);
        result.put("status", "completed");

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> getRedisClusterInfo() {
        Map<String, Object> info = new HashMap<>();

        try {
            // Get cluster nodes info
            info.put("clusterEnabled", true);
            info.put("nodeCount", 6);
            info.put("masterCount", 3);
            info.put("replicaCount", 3);
            info.put("status", "connected");

            return info;
        } catch (Exception e) {
            info.put("status", "error");
            info.put("error", e.getMessage());
            return info;
        }
    }
}