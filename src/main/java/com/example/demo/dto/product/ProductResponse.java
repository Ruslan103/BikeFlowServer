package com.example.demo.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
        Long id, String name, String article, String category, String unit, String description,
        boolean active, BigDecimal currentPrice, LocalDate priceValidFrom, LocalDate priceValidTo
) { }
