package com.example.demo.dto.discount;

import java.math.BigDecimal;

public record DiscountResponse(Long id, String name, BigDecimal percentage) {
}
