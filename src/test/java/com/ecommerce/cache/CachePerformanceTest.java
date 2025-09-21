package com.ecommerce.cache;

import com.ecommerce.cache.cache.RedisCacheService;
import com.ecommerce.cache.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
@ActiveProfiles("test")
public class CachePerformanceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Test
    public void testCachePerformanceUnderLoad() throws InterruptedException {
        int threadCount = 50;
        int operationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong successfulOperations = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        // Submit concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String productId = "perf-test-" + (threadId * operationsPerThread + j);

                        // Mix of operations
                        if (j % 4 == 0) {
                            // 25% writes (cache population)
                            productService.getProduct(productId);
                        } else {
                            // 75% reads (should hit cache)
                            String cachedProductId = "perf-test-" + (j % 100);
                            productService.getProduct(cachedProductId);
                        }

                        totalOperations.incrementAndGet();
                        successfulOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Error in thread " + threadId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Print results
        System.out.println("=== CACHE PERFORMANCE TEST RESULTS ===");
        System.out.println("Total Operations: " + totalOperations.get());
        System.out.println("Successful Operations: " + successfulOperations.get());
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Operations/sec: " + (totalOperations.get() * 1000.0 / duration));
        System.out.println("Cache Stats: " + redisCacheService.getStats());
        System.out.println("=====================================");
    }
}