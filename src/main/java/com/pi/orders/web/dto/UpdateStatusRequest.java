package com.pi.orders.web.dto;

import com.pi.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull OrderStatus status) {
}
