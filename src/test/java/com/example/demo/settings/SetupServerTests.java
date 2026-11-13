package com.example.demo.settings;

import com.example.demo.DemoApplication;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class SetupServerTests {
    @TempDir Path directory;
    private final JsonMapper json = JsonMapper.builder().build();
    private String csrfHeaderName = "X-CSRF-TOKEN";

    @Test
    void setupRunsWithoutDatabaseAuthenticatesSavesAndLoadsConfigurationOnRestart() throws Exception {
        Path settingsFile = directory.resolve("server-settings.properties");
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        try (var context = start(settingsFile)) {
            assertTrue(context.getBeansOfType(DataSource.class).isEmpty());
            assertEquals("127.0.0.1", context.getEnvironment().getProperty("server.address"));
            String base = "http://127.0.0.1:" + context.getEnvironment().getRequiredProperty("local.server.port");
            assertEquals(401, send(client, base, "/api/admin/settings", "GET", null, null).statusCode());
            assertEquals(200, send(client, base, "/login", "GET", null, null).statusCode());
            assertEquals(200, send(client, base, "/admin-assets/admin.css", "GET", null, null).statusCode());
            String token = csrf(client, base);
            var rejectedLogin = send(client, base, "/api/auth/login", "POST",
                    "{\"login\":\"setup-admin\",\"password\":\"incorrect\"}", token);
            assertEquals(401, rejectedLogin.statusCode(), rejectedLogin.body());
            token = csrf(client, base);
            var login = send(client, base, "/api/auth/login", "POST",
                    "{\"login\":\"setup-admin\",\"password\":\"setup-test-password\"}", token);
            assertEquals(200, login.statusCode(), login.body());
            assertTrue(cookies.getCookieStore().getCookies().stream()
                    .anyMatch(cookie -> cookie.getName().equals("BIKE_SETUP_SESSION")));
            assertEquals(200, send(client, base, "/admin", "GET", null, null).statusCode());
            String body = """
                    {"databaseUrl":"jdbc:postgresql://localhost:5432/saved",
                     "databaseUsername":"postgres","databasePassword":"test-db-password",
                     "trelloApiKey":"test-trello-key","trelloToken":"test-trello-token"}
                    """;
            assertEquals(403, send(client, base, "/api/admin/settings", "PUT", body, null).statusCode());
            var saved = send(client, base, "/api/admin/settings", "PUT", body, csrf(client, base));
            assertEquals(200, saved.statusCode(), saved.body());
            assertFalse(saved.body().contains("test-db-password"));
            assertFalse(saved.body().contains("test-trello-key"));
            assertFalse(saved.body().contains("test-trello-token"));
            var read = send(client, base, "/api/admin/settings", "GET", null, null);
            assertTrue(json.readTree(read.body()).get("trelloTokenConfigured").asBoolean());
            assertEquals(204, send(client, base, "/api/auth/logout", "POST", null, csrf(client, base)).statusCode());
            assertEquals(401, send(client, base, "/api/admin/settings", "GET", null, null).statusCode());
        }
        assertTrue(Files.exists(settingsFile));
        try (var restarted = start(settingsFile)) {
            assertEquals("jdbc:postgresql://localhost:5432/saved",
                    restarted.getEnvironment().getProperty("spring.datasource.url"));
            assertEquals("test-db-password", restarted.getEnvironment().getProperty("spring.datasource.password"));
            assertEquals("test-trello-token", restarted.getEnvironment().getProperty("bike.trello.token"));
            assertTrue(restarted.getBeansOfType(DataSource.class).isEmpty());
        }
    }

    private ConfigurableApplicationContext start(Path file) {
        var application = DemoApplication.createApplication(true);
        return application.run(
                "--BIKE_SETTINGS_FILE=" + file,
                "--BIKE_ADMIN_LOGIN=setup-admin",
                "--BIKE_ADMIN_PASSWORD=setup-test-password",
                "--server.port=0",
                "--server.address=127.0.0.1",
                "--server.ssl.enabled=false",
                "--server.servlet.session.cookie.secure=false",
                "--spring.main.banner-mode=off");
    }

    private String csrf(HttpClient client, String base) throws Exception {
        var response = send(client, base, "/api/auth/csrf", "GET", null, null);
        assertEquals(200, response.statusCode(), response.body());
        var csrfResponse = json.readTree(response.body());
        csrfHeaderName = csrfResponse.get("headerName").asText();
        return csrfResponse.get("token").asText();
    }

    private HttpResponse<String> send(HttpClient client, String base, String path,
                                      String method, String body, String csrf) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path))
                .header("Accept", "application/json");
        if (csrf != null) builder.header(csrfHeaderName, csrf);
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
