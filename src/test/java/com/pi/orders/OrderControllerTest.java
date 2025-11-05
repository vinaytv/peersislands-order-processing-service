package com.pi.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.orders.domain.OrderStatus;
import com.pi.orders.service.OrderService;
import com.pi.orders.web.OrderController;
import com.pi.orders.web.dto.CreateOrderRequest;
import com.pi.orders.web.dto.OrderItemRequest;
import com.pi.orders.web.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Thin controller tests — verify HTTP ↔️ service wiring, status codes, and query param → Pageable mapping.
 */
@WebMvcTest(controllers = OrderController.class)
public class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OrderService orderService;

    /* ---------- helpers ---------- */

    private OrderResponse sampleResponse(long id) {
        return new OrderResponse(
                id,
                "cust-1",
                List.of(new OrderResponse.Item("SKU-1", "Mouse", 1, new BigDecimal("499.99"), new BigDecimal("499.99"))),
                OrderStatus.PENDING,
                new BigDecimal("499.99"),
                OffsetDateTime.parse("2025-01-01T00:00:00Z").toInstant(),
                OffsetDateTime.parse("2025-01-01T00:00:00Z").toInstant()
        );
    }

    /* ---------- tests ---------- */

    @Test
    void createOrder_returns201_andBody() throws Exception {
        // given
        var req = new CreateOrderRequest(
                "cust-1",
                List.of(new OrderItemRequest("SKU-1", "Mouse", 1, new BigDecimal("499.99")))
        );
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(sampleResponse(42L));

        // when/then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.customerId").value("cust-1"));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    void getOrderDetails_returns200_andBody() throws Exception {
        when(orderService.getOrderDetails(7L)).thenReturn(sampleResponse(7L));

        mockMvc.perform(get("/api/orders/{id}", 7L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(orderService).getOrderDetails(7L);
    }

    @Test
    void cancel_returns200_andBody() throws Exception {
        when(orderService.cancelOrder(9L)).thenReturn(sampleResponse(9L));

        mockMvc.perform(patch("/api/orders/{id}/cancel", 9L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));

        verify(orderService).cancelOrder(9L);
    }

    @Test
    void listByCustomer_defaults_andSortDesc() throws Exception {
        // given defaults: page=0, size=20, sort=createdAt,desc
        Page<OrderResponse> page = new PageImpl<>(List.of(sampleResponse(1L)), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(orderService.listOrders(anyString(), anyList(), any(Pageable.class))).thenReturn(page);

        // when
        mockMvc.perform(get("/api/orders")
                        .param("customerId", "cust-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // then: capture pageable to assert mapping
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).listOrders(eq("cust-1"), isNull(), pageableCaptor.capture());

        Pageable p = pageableCaptor.getValue();
        assertThat(p.getPageNumber()).isEqualTo(0);
        assertThat(p.getPageSize()).isEqualTo(20);
        Sort.Order sort = p.getSort().getOrderFor("createdAt");
        assertThat(sort).isNotNull();
        assertThat(sort.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listByCustomer_customSortAsc_andStatuses() throws Exception {
        Page<OrderResponse> page = new PageImpl<>(List.of(sampleResponse(10L)));
        when(orderService.listOrders(anyString(), anyList(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/orders")
                        .param("customerId", "cust-2")
                        .param("status", "PENDING")
                        .param("status", "SHIPPED")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "updatedAt,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<List<OrderStatus>> statusesCaptor = ArgumentCaptor.forClass(List.class);

        verify(orderService).listOrders(eq("cust-2"), statusesCaptor.capture(), pageableCaptor.capture());

        // assert statuses
        assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(OrderStatus.PENDING, OrderStatus.SHIPPED);

        // assert pageable mapping
        Pageable p = pageableCaptor.getValue();
        assertThat(p.getPageNumber()).isEqualTo(2);
        assertThat(p.getPageSize()).isEqualTo(5);
        Sort.Order sort = p.getSort().getOrderFor("updatedAt");
        assertThat(sort).isNotNull();
        assertThat(sort.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void createOrder_validationFails_returns400() throws Exception {
        // Example invalid body: missing items (assuming @Size(min=1) on items)
        var invalid = new CreateOrderRequest("cust-1", List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
