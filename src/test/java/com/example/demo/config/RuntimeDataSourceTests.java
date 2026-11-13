package com.example.demo.config;

import java.sql.DriverManager;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeDataSourceTests {
    @Test
    void switchesNewConnectionsOnlyAfterTargetAdminIsValidated() throws Exception {
        String sourceUrl = "jdbc:h2:mem:runtime_source;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        String targetUrl = "jdbc:h2:mem:runtime_target;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        initializeSource(sourceUrl);
        initializeTarget(targetUrl);
        var dataSource = new RuntimeDataSource(sourceUrl, "sa", "");
        try {
            dataSource.switchTo(targetUrl, "sa", "", 7L);
            try (var connection = dataSource.getConnection()) {
                assertTrue(connection.getMetaData().getURL().contains("runtime_target"));
            }
            String invalidTarget = "jdbc:h2:mem:runtime_invalid;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
            assertThrows(IllegalStateException.class, () ->
                    dataSource.switchTo(invalidTarget, "sa", "", 7L));
            try (var connection = dataSource.getConnection()) {
                assertTrue(connection.getMetaData().getURL().contains("runtime_target"));
            }
        } finally {
            dataSource.close();
        }
    }

    private void initializeSource(String url) throws Exception {
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE startup_probe (id BIGINT PRIMARY KEY)");
        }
    }

    private void initializeTarget(String url) throws Exception {
        DataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE user_accounts (id BIGINT PRIMARY KEY, enabled BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE roles (id BIGINT PRIMARY KEY, code VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE user_roles (user_account BIGINT NOT NULL, role BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE spring_session (primary_id CHAR(36) PRIMARY KEY)");
        jdbc.execute("CREATE TABLE spring_session_attributes (session_primary_id CHAR(36) NOT NULL)");
        jdbc.update("INSERT INTO user_accounts(id, enabled) VALUES (7, TRUE)");
        jdbc.update("INSERT INTO roles(id, code) VALUES (1, 'ADMIN')");
        jdbc.update("INSERT INTO user_roles(user_account, role) VALUES (7, 1)");
    }
}
