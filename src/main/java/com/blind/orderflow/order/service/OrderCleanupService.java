package com.blind.orderflow.order.service;

import com.blind.orderflow.order.repository.OrderRepository;
import com.blind.orderflow.shared.utils.enums.OrderStatus;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupService {


    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedRate = 60000)
    public void cancelStaleOrders() {

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

        orderRepository.findByStatus(OrderStatus.PENDING_PAYMENT)
                .filter(order -> order.getCreatedAt().isBefore(cutoff))
                .flatMap(order ->
                        orderService.cancelOrder(
                                order.getOrderId(),
                                "payment_timeout",
                                "system-cleanup-" + UUID.randomUUID()
                        )
                )
                .doOnNext(r ->
                        Logger.info(
                                r.getOrderId(),
                                "ORDER",
                                "ORDER_CANCELLED",
                                "INFO",
                                "Cancelled stale order due to payment timeout"
                        ))
                .subscribe();
    }
}
