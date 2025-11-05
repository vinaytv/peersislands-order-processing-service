package com.pi.orders.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @Size(min = 1) List<OrderItemRequest> items
) {
}
