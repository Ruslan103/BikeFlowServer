package com.example.demo.dto.workorder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ServiceQuoteLineRequest(
        @NotNull Long serviceId, Long discountId,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceWithoutDiscount,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceWithDiscount,
        @Size(max = 150) String serviceNameSnapshot) { }
