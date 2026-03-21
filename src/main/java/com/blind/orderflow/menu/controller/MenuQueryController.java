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

    @GetMapping("/tags/{tag}")
    public Mono<?> getMenuByTags(@PathVariable String tag) {
        return menuService.getByTag(tag)
                .collectList()
                .flatMap(ResponseFactory::successWithContext);

    }

    @GetMapping("/category/{category}")
    public Mono<?> getMenuByCategory(@PathVariable String category) {
        return menuService.getByCategory(category)
                .collectList()
                .flatMap(ResponseFactory::successWithContext);

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

    @GetMapping("/paged")
    public Mono<?> getMenuPaged(
            @RequestParam(defaultValue = "0") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false)String category,
            @RequestParam(required = false)String tag
    ) {

        return menuService.getMenuPaged(pageSize, currentPage,category,tag)
                .flatMap(ResponseFactory::successWithContext);
    }
}