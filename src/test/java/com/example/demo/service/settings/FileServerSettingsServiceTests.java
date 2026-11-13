package com.example.demo.service.settings;

import com.example.demo.dto.settings.ServerSettingsRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class FileServerSettingsServiceTests {
    @TempDir Path directory;

    private FileServerSettingsService service() {
        return new FileServerSettingsService(new MockEnvironment()
                .withProperty("BIKE_SETTINGS_FILE", directory.resolve("settings.properties").toString())
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/current")
                .withProperty("spring.datasource.username", "postgres")
                .withProperty("spring.datasource.password", "original-password"),
                new DefaultListableBeanFactory().getBeanProvider(
                        com.example.demo.config.RuntimeDataSource.class));
    }

    @Test
    void savePreservesSecretsOnBlankFieldsAndExplicitlyClearsThem() throws Exception {
        var service = service();
        var saved = service.save(request("new-password", false, "test-key", false, "test-token", false), 1L);
        assertTrue(saved.databaseRestartRequired());
        assertFalse(saved.loginAgainRequired());
        assertTrue(saved.databasePasswordConfigured());
        assertTrue(saved.trelloApiKeyConfigured());
        service.save(request("", false, null, false, "", false), 1L);
        var properties = load();
        assertEquals("new-password", properties.getProperty("spring.datasource.password"));
        assertEquals("test-key", properties.getProperty("bike.trello.api-key"));
        assertEquals("test-token", properties.getProperty("bike.trello.token"));
        saved = service.save(request(null, true, null, true, null, true), 1L);
        assertFalse(saved.databasePasswordConfigured());
        assertFalse(saved.trelloApiKeyConfigured());
        assertFalse(saved.trelloTokenConfigured());
        assertEquals("", load().getProperty("bike.trello.token"));
    }

    @Test
    void firstSaveRetainsExistingDatabasePasswordIfNotReplaced() throws Exception {
        service().save(request(null, false, null, false, null, false), 1L);
        assertEquals("original-password", load().getProperty("spring.datasource.password"));
    }

    @Test
    void invalidUrlOrConflictingSecretDoesNotOverwriteExistingFile() throws Exception {
        var service = service();
        service.save(request("password", false, "key", false, "token", false), 1L);
        byte[] original = Files.readAllBytes(directory.resolve("settings.properties"));
        assertThrows(ResponseStatusException.class, () ->
                service.save(request("password", true, null, false, null, false), 1L));
        assertThrows(ResponseStatusException.class, () -> service.save(new ServerSettingsRequest(
                "jdbc:postgresql://localhost/bike?password=secret", "postgres", null, false,
                null, false, null, false), 1L));
        assertArrayEquals(original, Files.readAllBytes(directory.resolve("settings.properties")));
    }

    @Test
    void valuesSurviveServiceRecreationAndPropertiesEscaping() throws Exception {
        String secret = " p:a=s#s\\word ";
        service().save(request(secret, false, "ключ", false, "token", false), 1L);
        assertEquals(secret, load().getProperty("spring.datasource.password"));
        assertEquals("ключ", load().getProperty("bike.trello.api-key"));
        assertTrue(service().read().trelloTokenConfigured());
    }

    private ServerSettingsRequest request(String password, boolean clearPassword,
                                          String key, boolean clearKey,
                                          String token, boolean clearToken) {
        return new ServerSettingsRequest("jdbc:postgresql://localhost:5432/bike", "postgres",
                password, clearPassword, key, clearKey, token, clearToken);
    }

    private Properties load() throws Exception {
        var properties = new Properties();
        try (var input = Files.newInputStream(directory.resolve("settings.properties"))) {
            properties.load(input);
        }
        return properties;
    }
}
