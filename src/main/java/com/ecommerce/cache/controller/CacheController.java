package com.ecommerce.cache.controller;

import com.ecommerce.cache.cache.RedisCacheService;
import com.ecommerce.cache.model.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final RedisCacheService redisCacheService;

    @GetMapping("/stats")
    public ResponseEntity<CacheStats> getCacheStats() {
        return ResponseEntity.ok(redisCacheService.getStats());
    }

    @PostMapping("/stats/reset")
    public ResponseEntity<Void> resetCacheStats() {
        redisCacheService.resetStats();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> invalidateProduct(@PathVariable String productId) {
        redisCacheService.invalidateProduct(productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> invalidateSession(@PathVariable String sessionId) {
        redisCacheService.invalidateUserSession(sessionId);
        return ResponseEntity.ok().build();
    }
}