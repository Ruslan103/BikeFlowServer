package com.example.demo.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductPriceResponse(Long id, Long productId, BigDecimal price, LocalDate validFrom, LocalDate validTo) { }
