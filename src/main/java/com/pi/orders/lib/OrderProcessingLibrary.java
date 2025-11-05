package com.pi.orders.lib;

import com.pi.orders.domain.Order;
import com.pi.orders.web.dto.OrderResponse;

public class OrderProcessingLibrary {

    public static OrderResponse toResponse(Order o) {
        var items = o.getItems().stream().map(i -> new OrderResponse.Item(i.getSku(), i.getName(), i.getQuantity(), i.getUnitPrice(), i.lineTotal())).toList();
        return new OrderResponse(o.getId(), o.getCustomerId(), items, o.getStatus(), o.total(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
