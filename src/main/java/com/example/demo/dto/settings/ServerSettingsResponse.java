package com.example.demo.dto.settings;

public record ServerSettingsResponse(
        String databaseUrl,
        String databaseUsername,
        boolean databasePasswordConfigured,
        boolean trelloApiKeyConfigured,
        boolean trelloTokenConfigured,
        boolean databaseRestartRequired,
        boolean loginAgainRequired,
        boolean setupMode
) {
}
