package com.example.demo.dto.client;

public record ClientBicycleResponse(Long id, Long brandId, String brandName, String comment, Long version) {
    public ClientBicycleResponse(Long id, Long brandId, String brandName, String comment) {
        this(id, brandId, brandName, comment, 0L);
    }
}
