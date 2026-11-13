package com.example.demo.dto;

import java.math.BigDecimal;

public record WorkOrderServiceItemResponse(
        Long serviceId,
        String serviceNameSnapshot,
        Long discountId,
        String discountName,
        BigDecimal discountPercentage,
        BigDecimal priceWithoutDiscount,
        BigDecimal priceWithDiscount
) {
}
