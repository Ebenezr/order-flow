package com.blind.orderflow.kitchen.service;

import com.blind.orderflow.kitchen.entity.KitchenOrder;
import com.blind.orderflow.kitchen.repository.KitchenOrderRepository;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.shared.dto.PaginatedResponse;
import com.blind.orderflow.shared.exceptions.NotFoundException;
import com.blind.orderflow.shared.utils.enums.OrderStatus;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenService {

    private final KitchenOrderRepository kitchenRepository;
    private final OrderService orderService;

    public Mono<KitchenOrder> createKitchenOrder(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        KitchenOrder order =
                KitchenOrder.builder()
                        .kitchenOrderId(UUID.randomUUID().toString())
                        .orderId(orderId)
                        .status("RECEIVED")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        Mono<KitchenOrder> pipeline= kitchenRepository.findFirstByOrderId(orderId)
                .flatMap(existing -> Mono.just(existing)) // skip
                .switchIfEmpty(
                        kitchenRepository.save(order)
                );
        return Logger.logMono(pipeline, "KITCHEN", "CREATE_KITCHEN_ORDER", start);
    }

    public  Mono<PaginatedResponse<KitchenOrder>> getKitchenOrders(int currentPage,int pageSize,String status) {

        LocalDateTime start = LocalDateTime.now();

        int safePageSize = pageSize <= 0 ? 10 : pageSize;
        int safePage = Math.max(currentPage, 0);

        int offset = safePage * safePageSize;


        Mono<List<KitchenOrder>> dataMono;
        Mono<Long> countMono;

        if(status != null && !status.isBlank()){

            dataMono= kitchenRepository.findPagedByStatus(status,pageSize,offset).collectList();
            countMono= kitchenRepository.countByStatus(status);
        }else{
            dataMono= kitchenRepository.findPaged(pageSize,offset).collectList();
            countMono= kitchenRepository.countAll();
        }



        Mono<PaginatedResponse<KitchenOrder>> pipeline=
                Mono.zip(dataMono, countMono)
                        .map(tuple -> {
                           List<KitchenOrder> items = tuple.getT1();
                           long total = tuple.getT2();
                           int totalPages = (int) Math.ceil((double) total / safePageSize );

                           return PaginatedResponse.<KitchenOrder>builder()
                                   .data(items)
                                   .totalPages(totalPages)
                                   .pageNumber(safePage)
                                   .pageSize(safePageSize)
                                   .totalCount(total)
                                   .hasMore(safePage < totalPages -1)
                                   .build();
                        });


        return Logger.logMono(pipeline, "KITCHEN", "GET_KITCHEN_ORDERS", start);

    }

    public Mono<KitchenOrder> startPreparing(String kitchenOrderId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<KitchenOrder> pipeline= kitchenRepository.findByKitchenOrderId(kitchenOrderId)

                .switchIfEmpty(
                        Mono.error(new NotFoundException("Kitchen order not found"))
                )

                .flatMap(order -> {

                    order.setStatus("PREPARING");
                    order.setUpdatedAt(LocalDateTime.now());

                    return kitchenRepository.save(order);
                }).flatMap(saved ->
                        orderService.updateOrderStatus(saved.getOrderId(), OrderStatus.PREPARING)
                                .thenReturn(saved)
                );
        return Logger.logMono(pipeline, "KITCHEN", "START_PREP", start);
    }

    public Mono<KitchenOrder> markReady(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<KitchenOrder> pipeline= kitchenRepository.findByKitchenOrderId(orderId)
                .switchIfEmpty(
                        Mono.error(new NotFoundException("Kitchen order not found"))
                )
                .flatMap(order -> {

                    order.setStatus("READY");
                    order.setUpdatedAt(LocalDateTime.now());

                    return kitchenRepository.save(order);
                }).flatMap(saved ->
                        orderService.updateOrderStatus(saved.getOrderId(), OrderStatus.READY)
                                .thenReturn(saved)
                );

        return Logger.logMono(pipeline, "KITCHEN", "MARK_READY", start);
    }


}