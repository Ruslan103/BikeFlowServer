package com.example.demo.dto.service;

import java.util.List;

public record WorkshopServiceBatchResponse(
        List<WorkshopServiceResponse> services,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
