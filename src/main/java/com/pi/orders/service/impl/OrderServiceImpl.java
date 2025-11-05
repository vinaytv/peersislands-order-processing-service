package com.pi.orders.service.impl;

import com.pi.orders.domain.Order;
import com.pi.orders.domain.OrderItem;
import com.pi.orders.domain.OrderStatus;
import com.pi.orders.exception.BadRequestException;
import com.pi.orders.exception.GenericException;
import com.pi.orders.exception.NotFoundException;
import com.pi.orders.lib.OrderProcessingLibrary;
import com.pi.orders.repo.OrderRepository;
import com.pi.orders.service.OrderService;
import com.pi.orders.web.dto.CreateOrderRequest;
import com.pi.orders.web.dto.OrderItemRequest;
import com.pi.orders.web.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    /**
     * Create a new order for the given request.
     * - Logs start/end with customerId & generated orderId.
     * - Wraps unexpected errors as 500.
     */
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req) {
        log.info("[createOrder] customerId={} items={}", req.customerId(), req.items().size());
        try {
            Order order = new Order();
            order.setCustomerId(req.customerId());

            List<OrderItem> items = req.items().stream().map(this::toItem).toList();
            Order finalOrder = order;
            items.forEach(i -> i.setOrder(finalOrder));
            order.setItems(items);

            order = orderRepository.save(order);
            log.info("[createOrder] success orderId={} customerId={}", order.getId(), req.customerId());
            return OrderProcessingLibrary.toResponse(order);
        } catch (Exception e) {
            log.error("[createOrder] failed customerId={} cause={}", req.customerId(), e.toString(), e);
            throw new GenericException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_CREATE_ORDER",
                    "Error while creating order", "Exception", e);
        }
    }

    /**
     * Fetch order details by id.
     * - Returns 404 when not found (NotFoundException).
     * - Uses readOnly transaction for performance.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(Long orderId) {
        log.info("[getOrderDetails] orderId={}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NotFoundException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                            "Order " + orderId + " not found", "Order " + orderId + " not found", null));

            log.info("[getOrderDetails] success orderId={} status={}", orderId, order.getStatus());
            return OrderProcessingLibrary.toResponse(order);
        } catch (NotFoundException e) {
            log.warn("[getOrderDetails] not-found orderId={}", orderId);
            throw e; // keep 404
        } catch (Exception e) {
            log.error("[getOrderDetails] failed orderId={} cause={}", orderId, e.toString(), e);
            throw new GenericException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_GET_ORDER",
                    "Error fetching order details", "Exception", e);
        }
    }

    /**
     * List orders for a customer, optionally filtering by statuses.
     * - If statuses is null/empty, fetch by customer only.
     * - Returns a Page mapped to OrderResponse.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(String customerId, List<OrderStatus> statuses, Pageable pageable) {
        log.info("[listOrders] customerId={} statuses={} page={} size={}",
                customerId, statuses, pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<Order> page = orderRepository.findByCustomerIdAndStatusIn(customerId, statuses, pageable);

            log.info("[listOrders] result customerId={} totalElements={} totalPages={}",
                    customerId, page.getTotalElements(), page.getTotalPages());

            return page.map(OrderProcessingLibrary::toResponse);
        } catch (Exception e) {
            log.error("[listOrders] failed customerId={} cause={}", customerId, e.toString(), e);
            throw new GenericException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_LIST_ORDERS",
                    "Error while listing orders", "Exception", e);
        }
    }

    /**
     * Promote all PENDING orders to PROCESSING.
     * - Intended for a scheduled job; minimal logging.
     * - Uses saveAll for batch persistence.
     */
    @Override
    @Transactional
    public int updateOrders() {
        try {
            List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);
            pending.forEach(o -> o.setStatus(OrderStatus.PROCESSING));
            orderRepository.saveAll(pending);
            int count = pending.size();
            log.info("[updateOrders] promoted PENDING->PROCESSING count={}", count);
            return count;
        } catch (Exception e) {
            log.error("[updateOrders] failed cause={}", e.toString(), e);
            throw new GenericException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_PROMOTE_ORDERS",
                    "Error promoting pending orders", "Exception", e);
        }
    }

    /**
     * Cancel an order:
     * - Only allowed when status = PENDING, otherwise 400.
     * - 404 if order not found.
     */
    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        log.info("[cancelOrder] orderId={}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NotFoundException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                            "Order " + orderId + " not found", "Order " + orderId + " not found", null));

            if (order.getStatus() != OrderStatus.PENDING) {
                log.warn("[cancelOrder] not-pending orderId={} currentStatus={}", orderId, order.getStatus());
                throw new BadRequestException(HttpStatus.BAD_REQUEST, "ORDER_NOT_PENDING",
                        "Cannot cancel order unless it is in PENDING", "BusinessRule", null);
            }

            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);

            log.info("[cancelOrder] success orderId={} status={}", orderId, order.getStatus());
            return OrderProcessingLibrary.toResponse(order);
        } catch (NotFoundException e) {
            log.warn("[cancelOrder] not-found orderId={}", orderId);
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("[cancelOrder] failed orderId={} cause={}", orderId, e.toString(), e);
            throw new GenericException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_CANCEL_ORDER",
                    "Error cancelling order", "Exception", e);
        }
    }

    /* -------------------- helpers -------------------- */

    /**
     * Map an incoming DTO item to entity (without order back-reference).
     * The caller sets the Order relationship.
     */
    private OrderItem toItem(OrderItemRequest r) {
        OrderItem i = new OrderItem();
        i.setSku(r.sku());
        i.setName(r.name());
        i.setQuantity(r.quantity());
        i.setUnitPrice(r.unitPrice());
        return i;
    }
}
