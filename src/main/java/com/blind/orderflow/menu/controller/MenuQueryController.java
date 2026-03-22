package com.blind.orderflow.menu.controller;

import com.blind.orderflow.menu.service.MenuService;
import com.blind.orderflow.shared.utils.apis.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuQueryController {

    private final MenuService menuService;

    @GetMapping("/grouped/tag")
    public Mono<?> getGroupedTags() {
        return menuService.getMenuByTags()
                .flatMap(ResponseFactory::successWithContext);

    }

    @GetMapping("/grouped")
    public Mono<?> getGroupedByCategory() {
        return menuService.getMenuByCategory()
                .flatMap(ResponseFactory::successWithContext);
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

    @GetMapping()
    public Mono<?> getMenuPaged(
            @RequestParam(defaultValue = "0") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false)String category,
            @RequestParam(required = false)String tag,
            @RequestParam(defaultValue = "true")Boolean available
    ) {

        return menuService.getMenuPaged(pageSize, currentPage,category,tag,available)
                .flatMap(ResponseFactory::successWithContext);
    }
}