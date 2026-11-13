package com.example.demo.service.client;

import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(ClientSearchIntegrationTests.Config.class)
@Transactional
class ClientSearchIntegrationTests {
    @Configuration
    @EnableJpaRepositories(basePackages = "com.example.demo.repository")
    @Import(ClientSearchServiceImpl.class)
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:client_search;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPackagesToScan("com.example.demo.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ClientSearchService service;

    @BeforeEach
    void data() {
        jdbc.update("insert into contact_types(id,code,name) values (101,'PHONE','Телефон'),(102,'EMAIL','Почта')");
        jdbc.update("""
                insert into clients(id,public_id,last_name,first_name,middle_name,client_comment) values
                (101,1001,'Иванов','Иван','Иванович','Комментарий клиента'),
                (102,1002,'Петров','Пётр',null,null),
                (103,1003,'Сидоров','Сидор',null,null),
                (104,1004,'Яковлев','Яков',null,'Без велосипеда')
                """);
        jdbc.update("""
                insert into client_contacts(id,client,contact_type,contact_value,label) values
                (101,101,101,'+7 (999) 123-45-67','Личный'),
                (102,101,101,'79991234567','Второй'),
                (103,101,102,'client@example.test','Почта'),
                (104,102,101,'+8 (119) 123-45-67','Рабочий'),
                (105,103,102,'79991234567',null),
                (106,103,101,'79991234568',null),
                (107,104,101,'70091234567',null)
                """);
        jdbc.update("insert into bicycle_brands(id,name) values (101,'Cube')");
        jdbc.update("""
                insert into client_bicycles(id,client,brand,comment) values
                (101,101,101,'Красный'),(102,101,101,'Синий'),(103,102,101,null)
                """);
    }

    @Test
    void returnsUniqueClientsWithAllBicyclesContactsAndComments() {
        var result = service.findByPhone("+7 (999) 123-45-67");
        assertEquals(3, result.size());
        assertEquals(101L, result.getFirst().id());
        assertEquals(1001L, result.getFirst().publicId());
        assertEquals("Иванович", result.getFirst().middleName());
        assertEquals("Комментарий клиента", result.getFirst().comment());
        assertEquals(2, result.getFirst().bicycles().size());
        assertEquals("Cube", result.getFirst().bicycles().getFirst().brandName());
        assertEquals("Красный", result.getFirst().bicycles().getFirst().comment());
        assertEquals(3, result.getFirst().contacts().size());
        assertEquals("EMAIL", result.getFirst().contacts().get(2).typeCode());
        assertEquals("Личный", result.getFirst().contacts().getFirst().label());
        assertTrue(result.get(2).bicycles().isEmpty());
        assertEquals("Без велосипеда", result.get(2).comment());
    }

    @Test
    void ignoresPrefixButRequiresCompleteRemainingNumber() {
        assertEquals(service.findByPhone("79991234567"), service.findByPhone("00091234567"));
        assertTrue(service.findByPhone("7999123456").isEmpty());
        assertTrue(service.findByPhone("79990000000").isEmpty());
    }
}
