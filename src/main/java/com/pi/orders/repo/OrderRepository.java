package com.pi.orders.repo;

import com.pi.orders.domain.Order;
import com.pi.orders.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);

    @Query("""
               SELECT o FROM Order o
               WHERE o.customerId = :customerId
                 AND ( :#{#statuses == null || #statuses.isEmpty()} = true
                       OR o.status IN :statuses )
            """)
    Page<Order> findByCustomerIdAndStatusIn(@Param("customerId") String customerId,
                                            @Param("statuses") Collection<OrderStatus> statuses,
                                            Pageable pageable
    );

}
