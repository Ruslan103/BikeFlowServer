package com.example.demo.dto.trello;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateWorkOrderCardRequest(
        @NotBlank @Pattern(regexp = "[a-fA-F0-9]{24}") String listId,
        @Size(max = 100) List<@NotNull @Pattern(regexp = "[a-fA-F0-9]{24}") String> labelIds) {
    public CreateWorkOrderCardRequest {
        labelIds = labelIds == null ? List.of()
                : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(labelIds));
    }
}
