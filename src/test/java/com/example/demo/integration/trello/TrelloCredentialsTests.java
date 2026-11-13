package com.example.demo.integration.trello;

import com.example.demo.service.settings.FileServerSettingsService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class TrelloCredentialsTests {
    @TempDir Path directory;

    @Test
    void readsSavedCredentialsOnEveryRequestAndRespectsExplicitClear() throws Exception {
        Path file = directory.resolve("settings.properties");
        var environment = new MockEnvironment().withProperty("BIKE_SETTINGS_FILE", file.toString())
                .withProperty("bike.trello.api-key", "environment-key")
                .withProperty("bike.trello.token", "environment-token");
        var settings = new FileServerSettingsService(environment,
                new DefaultListableBeanFactory().getBeanProvider(com.example.demo.config.RuntimeDataSource.class));
        assertEquals("environment-token", settings.trelloCredentials().token());
        Files.writeString(file, "bike.trello.api-key=saved-key\nbike.trello.token=saved-token\n");
        assertEquals("saved-token", settings.trelloCredentials().token());
        Files.writeString(file, "bike.trello.api-key=saved-key\nbike.trello.token=\n");
        assertEquals("", settings.trelloCredentials().token());
    }
}
