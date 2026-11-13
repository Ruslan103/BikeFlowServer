package com.example.demo.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 80) String article,
        @Size(max = 100) String category,
        @NotBlank @Size(max = 30) String unit,
        String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        LocalDate validFrom
) { }
