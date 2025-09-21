package com.ecommerce.cache.service;

import com.ecommerce.cache.cache.RedisCacheService;
import com.ecommerce.cache.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final RedisCacheService redisCacheService;

    public Product getProduct(String productId) {
        log.debug("Getting product: {}", productId);

        Product cachedProduct = redisCacheService.getProduct(productId, Product.class);
        if (cachedProduct != null) {
            return cachedProduct;
        }

        Product product = fetchProductFromDatabase(productId);
        if (product != null) {
            // Cache the product
            redisCacheService.setProduct(productId, product);
        }

        return product;
    }

    public List<Product> getProductsByCategory(String category, int page, int size) {
        log.debug("Getting products by category: {} page: {} size: {}", category, page, size);

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String productId = String.format("%s-product-%d", category, (page * size) + i);
            Product product = getProduct(productId);
            if (product != null) {
                products.add(product);
            }
        }

        return products;
    }

    public Product updateProduct(String productId, Product updatedProduct) {
        log.debug("Updating product: {}", productId);

        updatedProduct.setId(productId);
        updatedProduct.setUpdatedAt(LocalDateTime.now());

        redisCacheService.invalidateProduct(productId);

        redisCacheService.setProduct(productId, updatedProduct);

        return updatedProduct;
    }

    public void deleteProduct(String productId) {
        log.debug("Deleting product: {}", productId);


        redisCacheService.invalidateProduct(productId);
    }

    public List<Product> searchProducts(String query, Map<String, Object> filters) {
        log.debug("Searching products with query: {} filters: {}", query, filters);

        List<Product> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String productId = "search-" + query + "-" + i;
            Product product = createMockProduct(productId);
            results.add(product);
        }

        return results;
    }

    private Product fetchProductFromDatabase(String productId) {
        log.debug("Fetching product from database: {}", productId);

        try {
            Thread.sleep(50 + ThreadLocalRandom.current().nextInt(100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Return mock product or null if not found
        if (productId.startsWith("nonexistent")) {
            return null;
        }

        return createMockProduct(productId);
    }

    private Product createMockProduct(String productId) {
        Random random = new Random(productId.hashCode());

        return Product.builder()
                .id(productId)
                .name("Product " + productId)
                .description("This is a detailed description for product " + productId)
                .category(getRandomCategory(random))
                .brand(getRandomBrand(random))
                .sku("SKU-" + productId.toUpperCase())
                .price(BigDecimal.valueOf(10 + random.nextDouble() * 990))
                .originalPrice(BigDecimal.valueOf(15 + random.nextDouble() * 1000))
                .stockQuantity(random.nextInt(100))
                .rating(3.0 + random.nextDouble() * 2)
                .reviewCount(random.nextInt(500))
                .images(Arrays.asList(
                        "https://example.com/images/" + productId + "-1.jpg",
                        "https://example.com/images/" + productId + "-2.jpg"
                ))
                .attributes(Map.of(
                        "weight", random.nextInt(5) + "kg",
                        "dimensions", random.nextInt(50) + "x" + random.nextInt(30) + "x" + random.nextInt(20),
                        "material", getRandomMaterial(random)
                ))
                .tags(Arrays.asList("popular", "trending", "new"))
                .active(random.nextBoolean())
                .featured(random.nextDouble() < 0.2) // 20% chance of being featured
                .createdAt(LocalDateTime.now().minusDays(random.nextInt(365)))
                .updatedAt(LocalDateTime.now().minusHours(random.nextInt(24)))
                .metaTitle(productId + " - Best Quality Product")
                .metaDescription("Buy " + productId + " at the best price with fast delivery")
                .slug(productId.toLowerCase().replace(" ", "-"))
                .build();
    }

    private String getRandomCategory(Random random) {
        String[] categories = {"Electronics", "Clothing", "Books", "Home & Garden", "Sports", "Toys"};
        return categories[random.nextInt(categories.length)];
    }

    private String getRandomBrand(Random random) {
        String[] brands = {"BrandA", "BrandB", "BrandC", "BrandD", "BrandE"};
        return brands[random.nextInt(brands.length)];
    }

    private String getRandomMaterial(Random random) {
        String[] materials = {"Cotton", "Plastic", "Metal", "Wood", "Glass"};
        return materials[random.nextInt(materials.length)];
    }
}