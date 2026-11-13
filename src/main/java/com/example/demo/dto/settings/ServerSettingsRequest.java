package com.example.demo.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServerSettingsRequest(
        @NotBlank @Size(max = 2048) String databaseUrl,
        @NotBlank @Size(max = 100) String databaseUsername,
        @Size(max = 1024) String databasePassword,
        Boolean clearDatabasePassword,
        @Size(max = 1024) String trelloApiKey,
        Boolean clearTrelloApiKey,
        @Size(max = 4096) String trelloToken,
        Boolean clearTrelloToken
) {
}
