package com.example.demo.dto.client;

import java.util.List;

public record ClientBatchResponse(
        List<ClientSearchResponse> clients,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
