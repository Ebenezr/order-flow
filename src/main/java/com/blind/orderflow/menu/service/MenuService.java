package com.blind.orderflow.menu.service;

import com.blind.orderflow.menu.entity.MenuItem;
import com.blind.orderflow.menu.repository.MenuRepository;
import com.blind.orderflow.shared.exceptions.NotFoundException;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    public Mono<MenuItem> getItem(String productId) {
        LocalDateTime start = LocalDateTime.now();
        Mono<MenuItem> pipeline=  menuRepository.findByProductId(productId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("Menu item not found: " + productId)
                ));
        return Logger.logMono(pipeline, "MENU", "GET_MENU_ITEM", start);
    }

    public Flux<MenuItem> getMenu() {

        LocalDateTime start = LocalDateTime.now();
        Flux<MenuItem> pipeline = menuRepository.findByAvailableTrue();
        return Logger.logFlux(pipeline, "MENU", "GET_MENU", start);
    }

    public Mono<MenuItem> create(MenuItem item) {

        LocalDateTime start = LocalDateTime.now();
        String productId = UUID.randomUUID().toString();

        item.setProductId(productId);
        item.setAvailable(true);


        Mono<MenuItem> pipeline= menuRepository.save(item)
                .doOnSuccess(saved ->
                        Logger.info(
                                productId,
                                "MENU",
                                "CREATE_ITEM",
                                "SUCCESS",
                                "Menu item created"
                        )
                );

        return Logger.logMono(pipeline, "MENU", "CREATE_MENU", start);
    }

    public Mono<MenuItem> update(String productId, MenuItem item) {

        LocalDateTime start = LocalDateTime.now();
        Mono<MenuItem> pipeline= getItem(productId)
                .flatMap(existing -> {

                    existing.setName(item.getName());
                    existing.setPrice(item.getPrice());
                    existing.setRecipe(item.getRecipe()); // important

                    return menuRepository.save(existing);
                });

        return Logger.logMono(pipeline, "MENU", "UPDATE_MENU", start);
    }

    public Mono<MenuItem> setAvailability(String productId, boolean available) {

        LocalDateTime start = LocalDateTime.now();

        Mono<MenuItem> pipeline= getItem(productId)
                .flatMap(existing -> {
                    existing.setAvailable(available);
                    return menuRepository.save(existing);
                });

        return Logger.logMono(pipeline, "MENU", "SET_MENU_AVAILABILITY", start);
    }
}