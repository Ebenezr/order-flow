package com.blind.orderflow.menu.controller;

import com.blind.orderflow.menu.entity.MenuItem;
import com.blind.orderflow.menu.service.MenuService;
import com.blind.orderflow.shared.utils.apis.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/admin/menu")
@RequiredArgsConstructor
public class MenuAdminController {

    private final MenuService menuService;

    @PostMapping("/items")
    public Mono<?> createMenuItem(@RequestBody MenuItem item) {

        return menuService.create(item)
                .flatMap(result ->
                        ResponseFactory.success(result, ResponseFactory.newRequestRefId())
                );
    }

    @PutMapping("/items/{productId}")
    public Mono<?> updateMenuItem(
            @PathVariable String productId,
            @RequestBody MenuItem item
    ) {

        return menuService.update(productId, item)
                .flatMap(result ->
                        ResponseFactory.success(result, ResponseFactory.newRequestRefId())
                );
    }

    @PutMapping("/items/{productId}/availability")
    public Mono<?> setAvailability(
            @PathVariable String productId,
            @RequestParam boolean available
    ) {

        return menuService.setAvailability(productId, available)
                .flatMap(result ->
                        ResponseFactory.success(result, ResponseFactory.newRequestRefId())
                );
    }
}