package com.pi.orders.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @Positive int quantity,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitPrice
) {
}
