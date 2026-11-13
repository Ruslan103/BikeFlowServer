package com.example.demo.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductQuoteRequest(
        @NotNull @Size(max = 1000) List<@NotNull @Valid ProductQuoteLineRequest> products
) { }
