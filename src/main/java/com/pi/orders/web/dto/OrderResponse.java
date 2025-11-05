package com.pi.orders.web.dto;

import com.pi.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerId,
        List<Item> items,
        OrderStatus status,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) {
    public record Item(String sku, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
