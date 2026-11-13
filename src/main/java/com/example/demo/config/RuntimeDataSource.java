package com.example.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;
import lombok.extern.slf4j.Slf4j;

/**
 * Switches the pool used by new transactions without restarting the web server.
 * Existing transactions finish against the pool from which they borrowed a connection.
 */
@Slf4j
public class RuntimeDataSource extends AbstractDataSource implements AutoCloseable {
    private final CopyOnWriteArrayList<HikariDataSource> retiringPools = new CopyOnWriteArrayList<>();
    private volatile HikariDataSource activePool;

    public RuntimeDataSource(String url, String username, String password) {
        log.info("Создаётся пул подключений к базе данных");
        this.activePool = createPool(url, username, password);
        log.info("Пул подключений к базе данных создан");
    }

    @Override
    public Connection getConnection() throws SQLException {
        log.debug("Запрошено подключение к базе данных");
        return activePool.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        log.debug("Запрошено подключение к базе данных с переданными параметрами");
        return activePool.getConnection(username, password);
    }

    public synchronized void switchTo(String url, String username, String password, long adminUserId) {
        log.info("Начата проверка нового подключения к базе данных");
        HikariDataSource nextPool = createPool(url, username, password);
        try (Connection connection = nextPool.getConnection()) {
            if (!connection.isValid(5)) {
                throw new IllegalStateException("PostgreSQL connection validation failed");
            }
            Integer adminCount = new JdbcTemplate(nextPool).queryForObject("""
                    SELECT COUNT(*)
                    FROM user_accounts account
                    JOIN user_roles assignment ON assignment.user_account = account.id
                    JOIN roles role ON role.id = assignment.role
                    WHERE account.id = ? AND account.enabled = TRUE AND role.code = 'ADMIN'
                    """, Integer.class, adminUserId);
            if (adminCount == null || adminCount != 1) {
                throw new IllegalStateException("The target database must contain the current active ADMIN account");
            }
            new JdbcTemplate(nextPool).queryForObject(
                    "SELECT COUNT(*) FROM spring_session", Integer.class);
            new JdbcTemplate(nextPool).queryForObject(
                    "SELECT COUNT(*) FROM spring_session_attributes", Integer.class);
        } catch (SQLException | RuntimeException exception) {
            nextPool.close();
            log.error("Новое подключение к базе данных не прошло проверку: {}",
                    exception.getClass().getSimpleName());
            throw new IllegalStateException("Cannot connect to the requested PostgreSQL database", exception);
        }

        try {
            ClientSearchDatabaseInitializer.initializeIfPostgres(nextPool);
        } catch (RuntimeException exception) {
            nextPool.close();
            throw exception;
        }

        HikariDataSource previousPool = activePool;
        activePool = nextPool;
        retiringPools.add(previousPool);
        retireWhenIdle(previousPool);
        log.info("Подключение к базе данных успешно переключено");
    }

    private HikariDataSource createPool(String url, String username, String password) {
        log.debug("Настраиваются параметры пула подключений к базе данных");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("bike-postgres");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    private void retireWhenIdle(HikariDataSource pool) {
        log.debug("Старый пул подключений ожидает завершения активных запросов");
        Thread thread = new Thread(() -> {
            try {
                while (!pool.isClosed() && pool.getHikariPoolMXBean().getActiveConnections() > 0) {
                    Thread.sleep(250);
                }
                pool.close();
                log.debug("Старый пул подключений закрыт");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.warn("Ожидание закрытия старого пула подключений прервано");
            } finally {
                retiringPools.remove(pool);
            }
        }, "retire-bike-datasource");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public synchronized void close() {
        log.info("Закрываются пулы подключений к базе данных");
        activePool.close();
        retiringPools.forEach(HikariDataSource::close);
        retiringPools.clear();
        log.info("Пулы подключений к базе данных закрыты");
    }
}
