package com.blind.orderflow.menu.repository;

import com.blind.orderflow.menu.entity.MenuItem;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MenuRepository
        extends ReactiveMongoRepository<MenuItem, String> {

    Mono<MenuItem> findByProductId(String productId);

    Flux<MenuItem> findByAvailableTrue();

    Flux<MenuItem> findByTagsContaining(String tag);

    Flux<MenuItem> findByCategoryAndAvailableTrue(String category);
}