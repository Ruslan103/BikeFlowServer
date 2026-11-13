package com.example.demo.dto.product;

import java.math.BigDecimal;

public record WorkOrderCustomerPartResponse(
        Long id, String name, String brand, String model, String serialNumber, BigDecimal quantity,
        String conditionOnReceipt, String reportedIssue, String includedAccessories
) { }
