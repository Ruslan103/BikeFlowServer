package com.example.demo.dto.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkshopServiceRequest(
        @NotBlank @Size(max = 150) String name,
        String description
) {
}
