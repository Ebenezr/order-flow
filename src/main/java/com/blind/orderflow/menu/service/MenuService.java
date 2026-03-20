package com.blind.orderflow.menu.service;

import com.blind.orderflow.menu.entity.MenuItem;
import com.blind.orderflow.menu.repository.MenuRepository;
import com.blind.orderflow.shared.exceptions.NotFoundException;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    public Mono<MenuItem> getItem(String productId) {
        return menuRepository.findByProductId(productId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("Menu item not found: " + productId)
                ));
    }

    public Flux<MenuItem> getMenu() {
//        Logger.info(
//
//        )
        return menuRepository.findByAvailableTrue();
    }

    public Mono<MenuItem> create(MenuItem item) {

        String productId = UUID.randomUUID().toString();

        item.setProductId(productId);
        item.setAvailable(true);

        Logger.info(
                productId,
                "MENU",
                "CREATE_ITEM",
                "START",
                "Creating menu item: " + item.getName()
        );

        return menuRepository.save(item)
                .doOnSuccess(saved ->
                        Logger.info(
                                productId,
                                "MENU",
                                "CREATE_ITEM",
                                "SUCCESS",
                                "Menu item created"
                        )
                );
    }

    public Mono<MenuItem> update(String productId, MenuItem item) {

        return getItem(productId)
                .flatMap(existing -> {

                    existing.setName(item.getName());
                    existing.setPrice(item.getPrice());
                    existing.setRecipe(item.getRecipe()); // important

                    return menuRepository.save(existing);
                });
    }

    public Mono<MenuItem> setAvailability(String productId, boolean available) {

        return getItem(productId)
                .flatMap(existing -> {
                    existing.setAvailable(available);
                    return menuRepository.save(existing);
                });
    }
}