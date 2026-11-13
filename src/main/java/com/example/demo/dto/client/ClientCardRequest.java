package com.example.demo.dto.client;

import com.example.demo.model.client.ClientType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClientCardRequest(@NotNull ClientType type, Long version,
        @Size(max = 100) String firstName, @Size(max = 100) String lastName,
        @Size(max = 100) String middleName, @Size(max = 250) String organizationName,
        @Size(max = 250) String representative, String comment,
        @NotNull List<@NotNull @Valid ContactInput> contacts, boolean allowDuplicateContacts) {
    public record ContactInput(Long id, @NotNull Long typeId,
            @jakarta.validation.constraints.NotBlank @Size(max = 255) String value, @Size(max = 100) String label) { }
}
