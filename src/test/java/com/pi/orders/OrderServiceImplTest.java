package com.pi.orders;

import com.pi.orders.domain.Order;
import com.pi.orders.domain.OrderItem;
import com.pi.orders.domain.OrderStatus;
import com.pi.orders.exception.BadRequestException;
import com.pi.orders.exception.GenericException;
import com.pi.orders.exception.NotFoundException;
import com.pi.orders.repo.OrderRepository;
import com.pi.orders.service.impl.OrderServiceImpl;
import com.pi.orders.web.dto.CreateOrderRequest;
import com.pi.orders.web.dto.OrderItemRequest;
import com.pi.orders.web.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl service; // class under test

    private CreateOrderRequest createReq;

    @BeforeEach
    void setUp() {
        createReq = new CreateOrderRequest(
                "cust-1",
                List.of(new OrderItemRequest("SKU-1", "Mouse", 1, new BigDecimal("499.99")))
        );
    }

    /* ---------- createOrder ---------- */

    @Test
    void createOrder_persistsAndReturnsResponse() {
        // given a new Order that repository will save and assign an ID
        Order saved = new Order();
        saved.setId(42L);
        saved.setCustomerId("cust-1");
        saved.setStatus(OrderStatus.PENDING);
        // items are set by service; repo returns saved entity
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        // when
        OrderResponse resp = service.createOrder(createReq);

        // then
        assertThat(resp.id()).isEqualTo(42L);
        assertThat(resp.customerId()).isEqualTo("cust-1");
        verify(orderRepository).save(any(Order.class));

        // capture to verify back-reference is set
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order toSave = captor.getValue();
        assertThat(toSave.getItems()).hasSize(1);
        OrderItem item = toSave.getItems().get(0);
        assertThat(item.getOrder()).isSameAs(toSave);
        assertThat(item.getSku()).isEqualTo("SKU-1");
    }

    @Test
    void createOrder_wrapsUnexpectedErrorsAs500() {
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.createOrder(createReq))
                .isInstanceOf(GenericException.class)
                .hasMessageContaining("Error while creating order");
    }

    /* ---------- getOrderDetails ---------- */

    @Test
    void getOrderDetails_returnsResponse_whenFound() {
        Order persisted = new Order();
        persisted.setId(7L);
        persisted.setCustomerId("cust-1");
        persisted.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(persisted));

        OrderResponse resp = service.getOrderDetails(7L);

        assertThat(resp.id()).isEqualTo(7L);
        verify(orderRepository).findById(7L);
    }

    @Test
    void getOrderDetails_throws404_whenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrderDetails(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");
    }

    /* ---------- listOrders ---------- */

    @Test
    void listOrders_callsRepo_andMapsPage() {
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Order o = new Order();
        o.setId(11L);
        o.setCustomerId("cust-1");
        o.setStatus(OrderStatus.SHIPPED);

        Page<Order> repoPage = new PageImpl<>(List.of(o), pageable, 1);
        when(orderRepository.findByCustomerIdAndStatusIn(eq("cust-1"), anyList(), eq(pageable)))
                .thenReturn(repoPage);

        Page<OrderResponse> page = service.listOrders("cust-1",
                List.of(OrderStatus.PENDING, OrderStatus.SHIPPED), pageable);

        assertThat(page.getContent().get(0).id()).isEqualTo(11L);
        verify(orderRepository).findByCustomerIdAndStatusIn(eq("cust-1"), anyList(), eq(pageable));
    }

    /* ---------- updateOrders ---------- */


    @Test
    void updateOrders_wrapsUnexpectedErrorsAs500() {
        when(orderRepository.findByStatus(OrderStatus.PENDING))
                .thenThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> service.updateOrders())
                .isInstanceOf(GenericException.class)
                .hasMessageContaining("promoting pending orders");
    }

    /* ---------- cancelOrder ---------- */

    @Test
    void cancelOrder_setsCanceled_whenPending() {
        Order o = new Order();
        o.setId(5L);
        o.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = service.cancelOrder(5L);

        assertThat(resp.status()).isEqualTo(OrderStatus.CANCELED);
        verify(orderRepository).save(argThat(ord -> ord.getStatus() == OrderStatus.CANCELED));
    }

    @Test
    void cancelOrder_throws404_whenNotFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelOrder(404L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancelOrder_throws400_whenNotPending() {
        Order o = new Order();
        o.setId(6L);
        o.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(6L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.cancelOrder(6L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel order");
    }
}
