package com.blind.orderflow.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginatedResponse<T> {
    private List<T> data;
    private int pageNumber;
    private int pageSize;
    private long totalCount;
    private int totalPages;
    private boolean hasMore;
}