package com.ecommerce.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class EcommerceRedisCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceRedisCacheApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("========================================");
        log.info("E-commerce Redis Cache Application READY!");
        log.info("========================================");
        log.info("Available endpoints:");
        log.info("  Health: http://localhost:8080/actuator/health");
        log.info("  Metrics: http://localhost:8080/actuator/metrics");
        log.info("  Prometheus: http://localhost:8080/actuator/prometheus");
        log.info("  Cache Stats: http://localhost:8080/api/cache/stats");
        log.info("  Products: http://localhost:8080/api/products/{id}");
        log.info("  Redis Commander: http://localhost:8081");
        log.info("========================================");
    }
}