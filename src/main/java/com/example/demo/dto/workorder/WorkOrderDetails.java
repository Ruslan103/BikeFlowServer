package com.example.demo.dto.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable client and bicycle data submitted with a work order. */
public record WorkOrderDetails(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String middleName,
        @NotBlank @Size(max = 100) String bicycleBrand,
        String bicycleComment,
        Long phoneContactId, @Size(max = 255) String phone,
        Long otherContactId, @Size(max = 30) String otherTypeCode,
        @Size(max = 255) String otherValue) { }
