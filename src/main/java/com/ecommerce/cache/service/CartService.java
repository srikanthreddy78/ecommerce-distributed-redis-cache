package com.ecommerce.cache.service;

import com.ecommerce.cache.cache.RedisCacheService;
import com.ecommerce.cache.model.Cart;
import com.ecommerce.cache.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisCacheService redisCacheService;
    private final ProductService productService;

    public Cart getOrCreateCart(String userId, String sessionId) {
        Map<String, Object> session = redisCacheService.getUserSession(sessionId);
        if (session != null && session.containsKey("cartId")) {
            String cartId = session.get("cartId").toString();
            // In a real implementation, you'd fetch from database or L2 cache
            return createMockCart(cartId, userId, sessionId);
        }

        String cartId = UUID.randomUUID().toString();
        Cart cart = createMockCart(cartId, userId, sessionId);

        if (session != null) {
            redisCacheService.updateSessionField(sessionId, "cartId", cartId);
        }

        log.debug("Created new cart: {} for user: {}", cartId, userId);
        return cart;
    }

    public Cart addItemToCart(String sessionId, String productId, int quantity) {
        Map<String, Object> session = redisCacheService.getUserSession(sessionId);
        if (session == null) {
            throw new RuntimeException("Invalid session");
        }

        Product product = productService.getProduct(productId);
        if (product == null) {
            throw new RuntimeException("Product not found: " + productId);
        }

        Integer currentStock = redisCacheService.getInventory(productId);
        if (currentStock == null) {
            // Set initial inventory from product
            redisCacheService.setInventory(productId, product.getStockQuantity());
            currentStock = product.getStockQuantity();
        }

        if (currentStock < quantity) {
            throw new RuntimeException("Insufficient inventory for product: " + productId);
        }

        Long newStock = redisCacheService.decrementInventory(productId, quantity);
        log.info("Added {} of product {} to cart. New inventory: {}", quantity, productId, newStock);

        String userId = session.get("userId").toString();
        Cart cart = getOrCreateCart(userId, sessionId);

        Cart.CartItem newItem = Cart.CartItem.builder()
                .productId(productId)
                .productName(product.getName())
                .sku(product.getSku())
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .imageUrl(!product.getImages().isEmpty() ? product.getImages().get(0) : null)
                .build();

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }
        cart.getItems().add(newItem);

        recalculateCartTotals(cart);

        return cart;
    }

    public Cart removeItemFromCart(String sessionId, String productId) {
        Map<String, Object> session = redisCacheService.getUserSession(sessionId);
        if (session == null) {
            throw new RuntimeException("Invalid session");
        }

        String userId = session.get("userId").toString();
        Cart cart = getOrCreateCart(userId, sessionId);

        if (cart.getItems() != null) {
            cart.getItems().removeIf(item -> item.getProductId().equals(productId));
            recalculateCartTotals(cart);
        }

        log.debug("Removed product {} from cart for user: {}", productId, userId);
        return cart;
    }

    private void recalculateCartTotals(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            cart.setSubtotal(BigDecimal.ZERO);
            cart.setTax(BigDecimal.ZERO);
            cart.setShipping(BigDecimal.ZERO);
            cart.setTotal(BigDecimal.ZERO);
            return;
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(Cart.CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.08)); // 8% tax
        BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(50)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(9.99); // Free shipping over $50

        cart.setSubtotal(subtotal);
        cart.setTax(tax);
        cart.setShipping(shipping);
        cart.setTotal(subtotal.add(tax).add(shipping));
        cart.setUpdatedAt(LocalDateTime.now());
    }

    private Cart createMockCart(String cartId, String userId, String sessionId) {
        return Cart.builder()
                .id(cartId)
                .userId(userId)
                .sessionId(sessionId)
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .shipping(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }
}