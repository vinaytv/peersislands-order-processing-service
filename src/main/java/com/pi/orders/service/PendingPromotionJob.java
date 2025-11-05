package com.pi.orders.service;

import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class PendingPromotionJob {
    private final OrderService service;

    public PendingPromotionJob(OrderService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = "${orders.jobs.promote.fixed-rate-ms:300000}")
    @SchedulerLock(name = "PendingPromotionJob.promote",
            lockAtMostFor = "PT4M",
            lockAtLeastFor = "PT30S")
    public void promote() {
        int n = service.updateOrders();
        if (n > 0) log.info("Promoted {} orders PENDING -> PROCESSING", n);
    }
}
