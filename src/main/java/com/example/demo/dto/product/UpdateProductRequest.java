package com.example.demo.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 80) String article,
        @Size(max = 100) String category,
        @NotBlank @Size(max = 30) String unit,
        String description,
        boolean active
) { }
