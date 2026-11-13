package com.example.demo.dto;

import com.example.demo.dto.client.ClientSearchResponse;
import com.example.demo.dto.product.WorkOrderCustomerPartResponse;
import com.example.demo.dto.product.WorkOrderProductItemResponse;
import com.example.demo.dto.workorder.WorkOrderDetails;
import java.time.Instant;
import java.util.List;

public record WorkOrderResponse(
        String blankNumber,
        Long clientId,
        Long clientBicycleId,
        Long clientContactId,
        Instant createdAt,
        String reportedIssue,
        String clientComment,
        String internalComment,
        String contactMethod,
        String contactValue,
        String statusCode,
        String trelloCard,
        String trelloUrl,
        List<WorkOrderServiceItemResponse> services,
        String trelloSyncStatusCode,
        ClientSearchResponse client,
        WorkOrderDetails details,
        com.example.demo.model.workorder.WorkOrderClientSnapshot snapshot,
        List<WorkOrderProductItemResponse> products,
        List<WorkOrderCustomerPartResponse> customerParts,
        String bicycleConditionOnReceipt
) {
}
