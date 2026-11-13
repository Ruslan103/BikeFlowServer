package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WorkOrderServiceItemRequest(
        @NotNull Long serviceId,
        Long discountId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceWithDiscount,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceWithoutDiscount,
        @Size(max = 150) String serviceNameSnapshot
) {
    public WorkOrderServiceItemRequest(Long serviceId, Long discountId, BigDecimal priceWithDiscount) {
        this(serviceId, discountId, priceWithDiscount, null, null);
    }
}
