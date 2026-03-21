package com.blind.orderflow.menu.service;

import com.blind.orderflow.menu.entity.MenuItem;
import com.blind.orderflow.menu.repository.MenuRepository;
import com.blind.orderflow.shared.dto.PaginatedResponse;
import com.blind.orderflow.shared.exceptions.NotFoundException;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<PaginatedResponse<MenuItem>> getMenuPaged(int pageSize, int currentPage,String category,String tag) {

        LocalDateTime start = LocalDateTime.now();

        int skip = currentPage * pageSize;

        Criteria criteria = Criteria.where("available").is(true);

        if (category != null && !category.isBlank()) {
            criteria = criteria.and("category").is(category);
        }

        if (tag != null && !tag.isBlank()) {
            tag = tag.trim().toUpperCase();
            criteria = criteria.and("tags").is(tag);
        }

        Query query = new Query(criteria)
                .skip(skip)
                .limit(pageSize);

        query.with(Sort.by(Sort.Direction.ASC, "name"));

        Mono<List<MenuItem>> dataMono =
                mongoTemplate.find(query, MenuItem.class).collectList();

        Mono<Long> countMono =
                mongoTemplate.count(
                        new Query(criteria),
                        MenuItem.class
                );

        Mono<PaginatedResponse<MenuItem>> pipeline= Mono.zip(dataMono, countMono)
                .map(tuple -> {

                    List<MenuItem> data = tuple.getT1();
                    long total = tuple.getT2();

                    int totalPages = (int) Math.ceil((double) total / pageSize);

                    return PaginatedResponse.<MenuItem>builder()
                            .data(data)
                            .pageNumber(currentPage)
                            .pageSize(pageSize)
                            .totalCount(total)
                            .totalPages(totalPages)
                            .hasMore(currentPage < totalPages - 1)
                            .build();
                });
        return Logger.logMono(pipeline, "MENU", "GET_MENU_PAGED", start);
    }

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

    public Flux<MenuItem> getByTag(String tag) {
        return menuRepository.findByTagsContaining(tag);
    }

    public Flux<MenuItem> getByCategory(String category) {
        return menuRepository.findByCategoryAndAvailableTrue(category);
    }

    public Mono<Map<String, List<MenuItem>>> getMenuByCategory() {

        LocalDateTime start = LocalDateTime.now();

        Mono<Map<String, List<MenuItem>>> pipeline = menuRepository.findByAvailableTrue()
                .collectList()
                .map(items ->
                        items.stream()
                                .collect(Collectors.groupingBy(MenuItem::getCategory))
                );

        return Logger.logMono(pipeline, "MENU", "GET_MENU_BY_CATEGORY", start);
    }

    public Mono<Map<String, List<MenuItem>>> getMenuByTags() {

        LocalDateTime start = LocalDateTime.now();

        Mono<Map<String, List<MenuItem>>> pipeline = menuRepository.findByAvailableTrue()
                .collectList()
                .map(items ->
                        items.stream()
                                .flatMap(item ->
                                        item.getTags().stream()
                                                .map(tag -> Map.entry(tag, item))
                                )
                                .collect(Collectors.groupingBy(
                                        Map.Entry::getKey,
                                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                                ))
                );

        return Logger.logMono(pipeline, "MENU", "GET_MENU_BY_TAGS", start);
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

                    existing.setCategory(item.getCategory());
                    existing.setDescription(item.getDescription());
                    existing.setImageUrl(item.getImageUrl());
                    existing.setTags(item.getTags());
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