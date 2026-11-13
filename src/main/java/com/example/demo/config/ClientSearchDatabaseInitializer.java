package com.example.demo.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientSearchDatabaseInitializer implements ApplicationRunner {

    private final ObjectProvider<RuntimeDataSource> dataSources;

    @Override
    public void run(ApplicationArguments args) {
        RuntimeDataSource dataSource = dataSources.getIfAvailable();
        if (dataSource != null) {
            initializeIfPostgres(dataSource);
        }
    }

    public static void initializeIfPostgres(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_clients_first_name_trgm "
                        + "ON clients USING GIN (lower(coalesce(first_name, '')) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_clients_last_name_trgm "
                        + "ON clients USING GIN (lower(coalesce(last_name, '')) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_clients_middle_name_trgm "
                        + "ON clients USING GIN (lower(coalesce(middle_name, '')) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_clients_organization_name_trgm "
                        + "ON clients USING GIN (lower(coalesce(organization_name, '')) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_clients_representative_trgm "
                        + "ON clients USING GIN (lower(coalesce(representative, '')) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_services_name_trgm "
                        + "ON services USING GIN (lower(name) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_discounts_name_trgm "
                        + "ON discounts USING GIN (lower(name) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_clients_full_name_trgm "
                        + "ON clients USING GIN (lower(coalesce(last_name, '') || ' ' "
                        + "|| coalesce(first_name, '') || ' ' || coalesce(middle_name, '')) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_client_contacts_value_trgm "
                        + "ON client_contacts USING GIN (lower(coalesce(contact_value, '')) gin_trgm_ops)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_client_contacts_phone_trgm "
                        + "ON client_contacts USING GIN "
                        + "((substring(regexp_replace(contact_value, '[^0-9]', '', 'g') from 4)) gin_trgm_ops)");
            }
            log.info("Расширение и индексы для поиска клиентов готовы");
        } catch (SQLException exception) {
            log.error("Не удалось подготовить полнотекстовый поиск клиентов: {}",
                    exception.getClass().getSimpleName());
            throw new IllegalStateException("Не удалось подготовить поиск клиентов с опечатками.", exception);
        }
    }
}
