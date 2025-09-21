
package com.ecommerce.cache.cache;

import com.ecommerce.cache.model.CacheStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;


    @Value("${cache.tiers.l1-product.ttl:86400}")
    private long l1ProductTtl;

    @Value("${cache.tiers.l1-product.prefix:l1:product:}")
    private String l1ProductPrefix;

    @Value("${cache.tiers.l2-session.ttl:1800}")
    private long l2SessionTtl;

    @Value("${cache.tiers.l2-session.prefix:l2:session:}")
    private String l2SessionPrefix;

    @Value("${cache.tiers.l3-inventory.ttl:300}")
    private long l3InventoryTtl;

    @Value("${cache.tiers.l3-inventory.prefix:l3:inventory:}")
    private String l3InventoryPrefix;

    @Value("${cache.tiers.l4-analytics.ttl:3600}")
    private long l4AnalyticsTtl;

    @Value("${cache.tiers.l4-analytics.prefix:l4:analytics:}")
    private String l4AnalyticsPrefix;


    private final AtomicLong totalHits = new AtomicLong(0);
    private final AtomicLong totalMisses = new AtomicLong(0);
    private final AtomicLong totalSets = new AtomicLong(0);
    private final AtomicLong totalDeletes = new AtomicLong(0);

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // L1 Cache: Product Methods
    public <T> T getProduct(String productId, Class<T> clazz) {
        String key = l1ProductPrefix + productId;

        try {
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                totalHits.incrementAndGet();
                log.debug("L1 cache hit for product: {}", productId);
                return objectMapper.convertValue(cached, clazz);
            }

            totalMisses.incrementAndGet();
            log.debug("L1 cache miss for product: {}", productId);
            return null;

        } catch (Exception e) {
            log.error("Error getting product from L1 cache: {}", productId, e);
            totalMisses.incrementAndGet();
            return null;
        }
    }

    public void setProduct(String productId, Object productData) {
        String key = l1ProductPrefix + productId;

        try {
            redisTemplate.opsForValue().set(key, productData, Duration.ofSeconds(l1ProductTtl));
            totalSets.incrementAndGet();
            log.debug("L1 cache set for product: {}", productId);
        } catch (Exception e) {
            log.error("Error setting product in L1 cache: {}", productId, e);
        }
    }

    // L2 Cache: User Sessions

    public Map<String, Object> getUserSession(String sessionId) {
        String key = l2SessionPrefix + sessionId;

        try {
            Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);

            if (!cached.isEmpty()) {
                totalHits.incrementAndGet();
                log.debug("L2 cache hit for session: {}", sessionId);

                // Convert to String keys
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<Object, Object> entry : cached.entrySet()) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
                return result;
            }

            totalMisses.incrementAndGet();
            log.debug("L2 cache miss for session: {}", sessionId);
            return null;

        } catch (Exception e) {
            log.error("Error getting session from L2 cache: {}", sessionId, e);
            totalMisses.incrementAndGet();
            return null;
        }
    }

    public void setUserSession(String sessionId, Map<String, Object> sessionData) {
        String key = l2SessionPrefix + sessionId;

        try {
            redisTemplate.opsForHash().putAll(key, sessionData);
            redisTemplate.expire(key, Duration.ofSeconds(l2SessionTtl));

            totalSets.incrementAndGet();
            log.debug("L2 cache set for session: {}", sessionId);

        } catch (Exception e) {
            log.error("Error setting session in L2 cache: {}", sessionId, e);
        }
    }

    public void updateSessionField(String sessionId, String field, Object value) {
        String key = l2SessionPrefix + sessionId;

        try {
            redisTemplate.opsForHash().put(key, field, value);
            redisTemplate.expire(key, Duration.ofSeconds(l2SessionTtl));
            log.debug("L2 cache field updated for session: {} field: {}", sessionId, field);

        } catch (Exception e) {
            log.error("Error updating session field in L2 cache: {} field: {}", sessionId, field, e);
        }
    }

    // L3 Cache: Inventory
    public Integer getInventory(String productId) {
        String key = l3InventoryPrefix + productId;

        try {
            String cached = (String) redisTemplate.opsForValue().get(key);

            if (cached != null) {
                totalHits.incrementAndGet();
                log.debug("L3 cache hit for inventory: {}", productId);
                return Integer.parseInt(cached);
            }

            totalMisses.incrementAndGet();
            log.debug("L3 cache miss for inventory: {}", productId);
            return null;

        } catch (Exception e) {
            log.error("Error getting inventory from L3 cache: {}", productId, e);
            totalMisses.incrementAndGet();
            return null;
        }
    }

    public void setInventory(String productId, int quantity) {
        String key = l3InventoryPrefix + productId;

        try {
            redisTemplate.opsForValue().set(key, String.valueOf(quantity), Duration.ofSeconds(l3InventoryTtl));
            totalSets.incrementAndGet();
            log.debug("L3 cache set for inventory: {} = {}", productId, quantity);

        } catch (Exception e) {
            log.error("Error setting inventory in L3 cache: {}", productId, e);
        }
    }

    public Long decrementInventory(String productId, int amount) {
        String key = l3InventoryPrefix + productId;

        try {
            Long newValue = redisTemplate.opsForValue().increment(key, -amount);
            redisTemplate.expire(key, Duration.ofSeconds(l3InventoryTtl));
            log.debug("L3 cache decremented inventory: {} by {} = {}", productId, amount, newValue);
            return newValue;

        } catch (Exception e) {
            log.error("Error decrementing inventory in L3 cache: {}", productId, e);
            return null;
        }
    }

    // L4 Cache: Analytics
    @SuppressWarnings("unchecked")
    public <T> T getAnalytics(String metric, String date, Class<T> clazz) {
        String key = l4AnalyticsPrefix + date + ":" + metric;

        try {
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                totalHits.incrementAndGet();
                log.debug("L4 cache hit for analytics: {}:{}", date, metric);

                if (clazz == Map.class) {
                    return (T) cached;
                }
                return objectMapper.convertValue(cached, clazz);
            }

            totalMisses.incrementAndGet();
            log.debug("L4 cache miss for analytics: {}:{}", date, metric);
            return null;

        } catch (Exception e) {
            log.error("Error getting analytics from L4 cache: {}:{}", date, metric, e);
            totalMisses.incrementAndGet();
            return null;
        }
    }

    public void setAnalytics(String metric, String date, Object data) {
        String key = l4AnalyticsPrefix + date + ":" + metric;

        try {
            redisTemplate.opsForValue().set(key, data, Duration.ofSeconds(l4AnalyticsTtl));
            totalSets.incrementAndGet();
            log.debug("L4 cache set for analytics: {}:{}", date, metric);

        } catch (Exception e) {
            log.error("Error setting analytics in L4 cache: {}:{}", date, metric, e);
        }
    }

    // Cache Invalidation

    public void invalidateProduct(String productId) {
        String key = l1ProductPrefix + productId;

        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                totalDeletes.incrementAndGet();
                log.debug("L1 cache invalidated for product: {}", productId);
            }
        } catch (Exception e) {
            log.error("Error invalidating product cache: {}", productId, e);
        }
    }

    public void invalidateUserSession(String sessionId) {
        String key = l2SessionPrefix + sessionId;

        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                totalDeletes.incrementAndGet();
                log.debug("L2 cache invalidated for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error invalidating session cache: {}", sessionId, e);
        }
    }

    // Statistics
    public CacheStats getStats() {
        long totalRequests = totalHits.get() + totalMisses.get();
        double hitRate = totalRequests > 0 ? (double) totalHits.get() / totalRequests * 100 : 0.0;

        return CacheStats.builder()
                .hits(totalHits.get())
                .misses(totalMisses.get())
                .sets(totalSets.get())
                .deletes(totalDeletes.get())
                .totalRequests(totalRequests)
                .hitRate(String.format("%.2f%%", hitRate))
                .build();
    }

    public void resetStats() {
        totalHits.set(0);
        totalMisses.set(0);
        totalSets.set(0);
        totalDeletes.set(0);
        log.info("Cache statistics reset");
    }
}