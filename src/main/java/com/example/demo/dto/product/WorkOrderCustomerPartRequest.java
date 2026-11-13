package com.example.demo.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record WorkOrderCustomerPartRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String brand,
        @Size(max = 100) String model,
        @Size(max = 100) String serialNumber,
        @NotNull @DecimalMin("0.001") @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        String conditionOnReceipt,
        String reportedIssue,
        String includedAccessories
) { }
