package com.example.demo.dto.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkOrderDefaultCommentRequest(
        @NotBlank @Size(max = 10_000) String comment
) {
}
