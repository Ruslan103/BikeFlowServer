package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RuntimeDataSourceConfig {
    @Bean(destroyMethod = "close")
    @Primary
    RuntimeDataSource dataSource(Environment environment) {
        log.info("Настраивается подключение к базе данных");
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password", "");
        if (url == null || username == null) {
            log.error("Не заданы обязательные настройки подключения к базе данных");
            throw new IllegalStateException("PostgreSQL must be configured before normal application startup");
        }
        log.info("Настройки подключения к базе данных загружены");
        return new RuntimeDataSource(url, username, password);
    }
}
