package com.ecommerce.cache.service;


import com.ecommerce.cache.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final RedisCacheService redisCacheService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Scheduled(fixedRate = 300000)
    public void generateDailyAnalytics() {
        String today = LocalDate.now().format(dateFormatter);

        // Generate mock analytics data
        Map<String, Object> salesData = generateSalesAnalytics();
        Map<String, Object> userActivityData = generateUserActivityAnalytics();
        Map<String, Object> productViewsData = generateProductViewsAnalytics();

        // Store in L4 cache
        redisCacheService.setAnalytics("daily_sales", today, salesData);
        redisCacheService.setAnalytics("user_activity", today, userActivityData);
        redisCacheService.setAnalytics("product_views", today, productViewsData);

        log.debug("Generated daily analytics for: {}", today);
    }

    public Map<String, Object> getDailyAnalytics(String date, String metric) {
        Map<String, Object> data = redisCacheService.getAnalytics(metric, date, Map.class);

        if (data == null) {
            // Generate data if not in cache
            data = switch (metric) {
                case "daily_sales" -> generateSalesAnalytics();
                case "user_activity" -> generateUserActivityAnalytics();
                case "product_views" -> generateProductViewsAnalytics();
                default -> new HashMap<>();
            };

            if (!data.isEmpty()) {
                redisCacheService.setAnalytics(metric, date, data);
            }
        }

        return data;
    }

    private Map<String, Object> generateSalesAnalytics() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalSales", random.nextDouble(10000, 50000));
        analytics.put("totalOrders", random.nextInt(100, 500));
        analytics.put("averageOrderValue", random.nextDouble(50, 200));
        analytics.put("topCategories", Map.of(
                "Electronics", random.nextInt(50, 200),
                "Clothing", random.nextInt(30, 150),
                "Books", random.nextInt(20, 100)
        ));

        return analytics;
    }

    private Map<String, Object> generateUserActivityAnalytics() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("activeUsers", random.nextInt(1000, 5000));
        analytics.put("newRegistrations", random.nextInt(50, 200));
        analytics.put("sessionDuration", random.nextDouble(5, 30)); // minutes
        analytics.put("bounceRate", random.nextDouble(0.2, 0.6));

        return analytics;
    }

    private Map<String, Object> generateProductViewsAnalytics() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalViews", random.nextInt(5000, 20000));
        analytics.put("uniqueViews", random.nextInt(3000, 15000));
        analytics.put("topProducts", Map.of(
                "product-1", random.nextInt(100, 500),
                "product-2", random.nextInt(80, 400),
                "product-3", random.nextInt(60, 300)
        ));

        return analytics;
    }
}