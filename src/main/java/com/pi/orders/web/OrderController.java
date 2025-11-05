package com.pi.orders.web;

import com.pi.orders.domain.OrderStatus;
import com.pi.orders.service.OrderService;
import com.pi.orders.web.dto.CreateOrderRequest;
import com.pi.orders.web.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order for a customer.
     * <p>
     * Request body: {@link CreateOrderRequest}
     * Response: 201 Created with {@link OrderResponse} in JSON.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    /**
     * Get order details by ID.
     * <p>
     * Path: /api/orders/{id}
     * Response: 200 OK with {@link OrderResponse}; 404 if not found.
     */
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getOrderDetails(id));
    }

    /**
     * Cancel an order (allowed only when status = PENDING).
     * <p>
     * Path: /api/orders/{id}/cancel
     * Response: 200 OK with updated {@link OrderResponse}; 404 if not found; 400 if not PENDING.
     */
    @PatchMapping(path = "/{id}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderResponse> cancel(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    /**
     * List orders for a customer with optional status filter and pagination.
     * <p>
     * Path: /api/orders/by-customer/{customerId}
     * Query:
     * - status (repeatable): ?status=PENDING&status=SHIPPED
     * - page (default 0), size (default 20)
     * - sort (default "createdAt,desc"), format: field,dir
     * Response: 200 OK with a Page of {@link OrderResponse}.
     */
    @GetMapping
    public Page<OrderResponse> listByCustomer(
            @RequestParam(name = "customerId") String customerId,
            @RequestParam(name = "status", required = false) List<OrderStatus> statuses,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort) {

        String[] parts = sort.split(",", 2);
        Sort.Direction dir = (parts.length == 2 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, parts[0]));

        return orderService.listOrders(customerId, statuses, pageable);
    }
}
