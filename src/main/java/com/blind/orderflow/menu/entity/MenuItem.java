package com.blind.orderflow.menu.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("menu_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem {

    @Id
    private String id;

    private String productId;

    private String name;

    private Double price;

    private Boolean available;

    private String imageUrl;

    private String category;

    private List<String> tags;

    private String description;

    private List<Ingredient> recipe;
}