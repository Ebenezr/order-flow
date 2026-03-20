package com.blind.orderflow.menu.controller;

import com.blind.orderflow.menu.service.MenuService;
import com.blind.orderflow.shared.utils.apis.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuQueryController {

    private final MenuService menuService;

    @GetMapping
    public Mono<?> getMenu() {

        return menuService.getMenu()
                .collectList()
                .flatMap(menu ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                        ResponseFactory.success(menu, requestId))
                );
    }

    @GetMapping("/available")
    public Mono<?> getAvailableMenu() {

        return menuService.getMenu()
                .collectList()
                .flatMap(menu ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                        ResponseFactory.success(menu, requestId))
                );
    }

    @GetMapping("/{productId}")
    public Mono<?> getItem(@PathVariable String productId) {

        return menuService.getItem(productId)
                .flatMap(item ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                        ResponseFactory.success(item, requestId))
                );
    }
}