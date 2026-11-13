package com.example.demo.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BicycleCardRequest(Long version, @NotBlank @Size(max = 100) String brandName, String comment) { }
