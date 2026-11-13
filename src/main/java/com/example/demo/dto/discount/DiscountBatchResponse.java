package com.example.demo.dto.discount;

import java.util.List;

public record DiscountBatchResponse(
        List<DiscountResponse> discounts,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
