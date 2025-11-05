package com.pi.orders.service;

import com.pi.orders.domain.OrderStatus;
import com.pi.orders.web.dto.CreateOrderRequest;
import com.pi.orders.web.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest createOrderRequest);

    OrderResponse getOrderDetails(Long orderId);

    Page<OrderResponse> listOrders(String customerId, List<OrderStatus> orderStatusList, Pageable pageable);

    int updateOrders();

    OrderResponse cancelOrder(Long orderId);


}
