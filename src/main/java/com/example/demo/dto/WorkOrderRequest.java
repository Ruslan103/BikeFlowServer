package com.example.demo.dto;

import com.example.demo.dto.workorder.WorkOrderDetails;
import com.example.demo.dto.product.WorkOrderCustomerPartRequest;
import com.example.demo.dto.product.WorkOrderProductItemRequest;
import java.time.Instant;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WorkOrderRequest(
        Long clientId,
        Long clientBicycleId,
        Long clientContactId,
        String reportedIssue,
        String clientComment,
        String internalComment,
        String contactMethod,
        String contactValue,
        @NotNull List<@NotNull @Valid WorkOrderServiceItemRequest> services,
        List<@NotNull @Valid WorkOrderProductItemRequest> products,
        List<@NotNull @Valid WorkOrderCustomerPartRequest> customerParts,
        @NotBlank String statusCode,
        WorkOrderDetails details,
        @Size(max = 32) String blankNumber,
        Instant createdAt,
        SnapshotAction snapshotAction, Long expectedClientVersion, Long expectedBicycleVersion, String bicycleConditionOnReceipt
) {
    public enum SnapshotAction { KEEP, REFRESH }

    public WorkOrderRequest(Long clientId, Long clientBicycleId, Long clientContactId, String reportedIssue,
            String clientComment, String internalComment, String contactMethod, String contactValue,
            List<WorkOrderServiceItemRequest> services, String statusCode, WorkOrderDetails details,
            String blankNumber, Instant createdAt, SnapshotAction snapshotAction,
            Long expectedClientVersion, Long expectedBicycleVersion) {
        this(clientId, clientBicycleId, clientContactId, reportedIssue, clientComment, internalComment,
                contactMethod, contactValue, services, List.of(), List.of(), statusCode, details, blankNumber,
                createdAt, snapshotAction, expectedClientVersion, expectedBicycleVersion, null);
    }

    public WorkOrderRequest(Long clientId, Long clientBicycleId, Long clientContactId, String reportedIssue,
            String clientComment, String internalComment, String contactMethod, String contactValue,
            List<WorkOrderServiceItemRequest> services, String statusCode, WorkOrderDetails details, String blankNumber, Instant createdAt) {
        this(clientId, clientBicycleId, clientContactId, reportedIssue, clientComment, internalComment, contactMethod,
                contactValue, services, List.of(), List.of(), statusCode, details, blankNumber, createdAt,
                SnapshotAction.KEEP, null, null, null);
    }
    public WorkOrderRequest(Long clientId, Long clientBicycleId, Long clientContactId, String reportedIssue,
            String clientComment, String internalComment, String contactMethod, String contactValue,
            List<WorkOrderServiceItemRequest> services, String statusCode) {
        this(clientId, clientBicycleId, clientContactId, reportedIssue, clientComment, internalComment,
                contactMethod, contactValue, services, List.of(), List.of(), statusCode, null, null, null,
                SnapshotAction.KEEP, null, null, null);
    }

    public WorkOrderRequest {
        products = products == null ? List.of() : List.copyOf(products);
        customerParts = customerParts == null ? List.of() : List.copyOf(customerParts);
    }
}
