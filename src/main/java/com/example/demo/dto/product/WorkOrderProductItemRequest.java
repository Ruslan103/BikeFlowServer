package com.example.demo.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WorkOrderProductItemRequest(
        Long productId,
        Long discountId,
        @NotNull @DecimalMin(value = "0.001") @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceWithoutDiscount,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceWithDiscount,
        @jakarta.validation.Valid CreateProductRequest newProduct,
        com.example.demo.model.workorder.ProductSupplyStatus supplyStatus
) { }
