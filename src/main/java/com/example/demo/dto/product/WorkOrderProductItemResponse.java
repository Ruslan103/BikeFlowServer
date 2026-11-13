package com.example.demo.dto.product;

import java.math.BigDecimal;

public record WorkOrderProductItemResponse(
        Long id, Long productId, String productNameSnapshot, String articleSnapshot, String unitSnapshot,
        BigDecimal quantity, Long discountId, String discountName, BigDecimal discountPercentage,
        BigDecimal priceWithoutDiscount, BigDecimal priceWithDiscount, BigDecimal lineTotal, com.example.demo.model.workorder.ProductSupplyStatus supplyStatus
) { }
