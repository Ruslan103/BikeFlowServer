package com.example.demo.service.settings;

import com.example.demo.dto.settings.ServerSettingsRequest;
import com.example.demo.dto.settings.ServerSettingsResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.springframework.beans.factory.ObjectProvider;
import com.example.demo.config.RuntimeDataSource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileServerSettingsService implements ServerSettingsService, com.example.demo.integration.trello.TrelloCredentialsProvider {
    private static final String URL = "spring.datasource.url";
    private static final String USERNAME = "spring.datasource.username";
    private static final String PASSWORD = "spring.datasource.password";
    private static final String TRELLO_KEY = "bike.trello.api-key";
    private static final String TRELLO_TOKEN = "bike.trello.token";
    private final Environment environment;
    private final Path settingsFile;
    private volatile String activeUrl;
    private volatile String activeUsername;
    private volatile String activePassword;
    private final ObjectProvider<RuntimeDataSource> dataSources;

    public FileServerSettingsService(Environment environment, ObjectProvider<RuntimeDataSource> dataSources) {
        log.info("Инициализируется сервис настроек сервера");
        this.environment = environment;
        this.dataSources = dataSources;
        this.settingsFile = Path.of(environment.getProperty("BIKE_SETTINGS_FILE",
                "./config/server-settings.properties")).toAbsolutePath().normalize();
        this.activeUrl = environment.getProperty(URL, "");
        this.activeUsername = environment.getProperty(USERNAME, "");
        this.activePassword = environment.getProperty(PASSWORD, "");
    }

    @Override
    public synchronized ServerSettingsResponse read() {
        log.info("Настройки сервера запрошены для чтения");
        return response(load(), false);
    }

    @Override
    public synchronized com.example.demo.integration.trello.TrelloCredentials trelloCredentials() {
        Properties settings = load();
        return new com.example.demo.integration.trello.TrelloCredentials(
                settings.getProperty(TRELLO_KEY, environment.getProperty(TRELLO_KEY, "")),
                settings.getProperty(TRELLO_TOKEN, environment.getProperty(TRELLO_TOKEN, "")));
    }

    @Override
    public synchronized ServerSettingsResponse save(ServerSettingsRequest request, Long adminUserId) {
        log.info("Начато сохранение настроек сервера");
        validateUrl(request.databaseUrl());
        Properties settings = load();
        settings.setProperty(URL, request.databaseUrl().trim());
        settings.setProperty(USERNAME, request.databaseUsername().trim());
        updateSecret(settings, PASSWORD, request.databasePassword(), Boolean.TRUE.equals(request.clearDatabasePassword()));
        updateSecret(settings, TRELLO_KEY, request.trelloApiKey(), Boolean.TRUE.equals(request.clearTrelloApiKey()));
        updateSecret(settings, TRELLO_TOKEN, request.trelloToken(), Boolean.TRUE.equals(request.clearTrelloToken()));
        String nextUrl = settings.getProperty(URL);
        String nextUsername = settings.getProperty(USERNAME);
        String nextPassword = settings.getProperty(PASSWORD, "");
        RuntimeDataSource runtimeDataSource = dataSources.getIfAvailable();
        Properties previousSettings = load();
        boolean settingsFileExisted = Files.exists(settingsFile);
        boolean databaseChanged = runtimeDataSource != null
                && (!Objects.equals(activeUrl, nextUrl)
                || !Objects.equals(activeUsername, nextUsername)
                || !Objects.equals(activePassword, nextPassword));
        write(settings);
        try {
            if (databaseChanged) {
                log.info("Проверяется новое подключение к базе данных");
                if (adminUserId == null) {
                    throw new IllegalStateException("Live database switching is available after normal login");
                }
                runtimeDataSource.switchTo(nextUrl, nextUsername, nextPassword, adminUserId);
            }
        } catch (RuntimeException exception) {
            restoreSettings(previousSettings, settingsFileExisted);
            log.error("Не удалось применить новое подключение к базе данных: {}",
                    exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Не удалось переключить подключение. Проверьте базу и наличие в ней вашей активной учётной записи ADMIN.",
                    exception);
        }
        if (runtimeDataSource != null) {
            activeUrl = nextUrl;
            activeUsername = nextUsername;
            activePassword = nextPassword;
        }
        log.info("Настройки сервера успешно сохранены");
        return response(settings, databaseChanged && runtimeDataSource != null);
    }

    private void restoreSettings(Properties previousSettings, boolean existed) {
        if (existed) {
            write(previousSettings);
            return;
        }
        try {
            Files.deleteIfExists(settingsFile);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось восстановить прежний файл настроек.", exception);
        }
    }

    private void validateUrl(String value) {
        try {
            if (value == null || !value.startsWith("jdbc:postgresql://")) {
                throw new IllegalArgumentException();
            }
            URI uri = URI.create(value.trim().substring("jdbc:".length()));
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                    || uri.getPath() == null || uri.getPath().length() < 2
                    || (uri.getPort() != -1 && (uri.getPort() < 1 || uri.getPort() > 65535))
                    || uri.getQuery() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Укажите адрес вида jdbc:postgresql://localhost:5432/bike_server без параметров. Логин и пароль вводятся отдельно.");
        }
    }

    private void updateSecret(Properties settings, String key, String value, boolean clear) {
        if (clear && value != null && !value.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Нельзя одновременно задать и удалить секретное значение.");
        }
        if (clear) {
            settings.setProperty(key, "");
        } else if (value != null && !value.isEmpty()) {
            settings.setProperty(key, value);
        } else if (!settings.containsKey(key)) {
            settings.setProperty(key, environment.getProperty(key, ""));
        }
    }

    private Properties load() {
        Properties settings = new Properties();
        if (!Files.exists(settingsFile)) {
            return settings;
        }
        try (var input = Files.newInputStream(settingsFile)) {
            settings.load(input);
            return settings;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось прочитать настройки сервера.", exception);
        }
    }

    private void write(Properties settings) {
        Path temporary = null;
        try {
            Files.createDirectories(settingsFile.getParent());
            temporary = Files.createTempFile(settingsFile.getParent(), ".server-settings-", ".tmp");
            restrictAccess(temporary);
            try (var output = Files.newOutputStream(temporary)) {
                settings.store(output, "Bike server settings. Contains credentials; do not commit.");
            }
            try {
                Files.move(temporary, settingsFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, settingsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось сохранить настройки сервера.", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The settings file is already saved or the original write error is propagated.
                }
            }
        }
    }

    private void restrictAccess(Path path) throws IOException {
        var posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null) {
            posix.setPermissions(PosixFilePermissions.fromString("rw-------"));
        }
        var acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
            acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                    .setPrincipal(acl.getOwner())
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
        }
    }

    private ServerSettingsResponse response(Properties settings, boolean loginAgainRequired) {
        boolean setupMode = environment.getProperty("bike.setup-mode", Boolean.class, false);
        boolean runtimeDataSourceAvailable = dataSources.getIfAvailable() != null;
        boolean useActiveConnection = !setupMode && runtimeDataSourceAvailable;
        String configuredUrl = useActiveConnection ? activeUrl : settings.getProperty(URL, activeUrl);
        String url = displayUrl(configuredUrl);
        String username = useActiveConnection ? activeUsername : settings.getProperty(USERNAME, activeUsername);
        String password = useActiveConnection ? activePassword : settings.getProperty(PASSWORD, activePassword);
        boolean databaseChanged = !Objects.equals(configuredUrl, activeUrl)
                || !Objects.equals(username, activeUsername) || !Objects.equals(password, activePassword);
        boolean requiresRestart = databaseChanged
                && !runtimeDataSourceAvailable
                && !environment.getProperty("bike.setup-mode", Boolean.class, false);
        return new ServerSettingsResponse(url, username, !password.isEmpty(),
                !settings.getProperty(TRELLO_KEY, environment.getProperty(TRELLO_KEY, "")).isEmpty(),
                !settings.getProperty(TRELLO_TOKEN, environment.getProperty(TRELLO_TOKEN, "")).isEmpty(),
                requiresRestart, loginAgainRequired,
                setupMode);
    }

    private String displayUrl(String url) {
        if (url.isEmpty()) {
            return "jdbc:postgresql://localhost:5432/bike_server";
        }
        if (!url.startsWith("jdbc:postgresql://")) {
            return "";
        }
        try {
            URI uri = URI.create(url.substring("jdbc:".length()));
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath(), null, null).toString().replaceFirst("^postgresql:", "jdbc:postgresql:");
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return "";
        }
    }
}
