package com.example.demo.service.workorder;

import com.example.demo.dto.WorkOrderRequest;
import com.example.demo.dto.WorkOrderResponse;
import com.example.demo.dto.WorkOrderServiceItemRequest;
import com.example.demo.dto.workorder.WorkOrderDetails;
import com.example.demo.service.client.ClientSearchServiceImpl;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(FullWorkOrderSaveTests.Config.class)
class FullWorkOrderSaveTests {
    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "com.example.demo.repository")
    @Import({WorkOrderServiceImpl.class, WorkOrderSnapshotService.class, com.example.demo.service.client.ClientCardService.class, ClientSearchServiceImpl.class, WorkOrderNumberService.class})
    static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:full_save;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""); }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPackagesToScan("com.example.demo.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
    }
    @Autowired WorkOrderService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void fixtures() {
        for (String table : List.of("work_order_services", "work_orders", "client_contacts", "client_bicycles", "clients",
                "bicycle_brands", "contact_types", "service_prices", "services", "discounts", "work_order_statuses", "trello_sync_statuses")) {
            jdbc.update("delete from " + table);
        }
        jdbc.execute("create sequence if not exists client_public_id_seq start with 1000");
        jdbc.execute("create sequence if not exists work_order_number_seq start with 1000");
        jdbc.update("insert into contact_types(id,code,name) values (1,'PHONE','Телефон'),(2,'EMAIL','Почта'),(3,'TELEGRAM','Telegram')");
        jdbc.update("insert into work_order_statuses(id,code,name) values (1,'NEW','Новый')");
        jdbc.update("insert into trello_sync_statuses(id,code,name) values (1,'PENDING','Ожидание')");
        jdbc.update("insert into bicycle_brands(id,name) values (100,'Cube')");
        jdbc.update("insert into services(id,name) values (1,'Тормоза')");
        jdbc.update("insert into discounts(id,name,percentage) values (1,'Клиент',10)");
    }

    @Autowired com.example.demo.service.client.ClientCardService cards;
    @Autowired com.example.demo.service.client.ClientSearchService search;
    @Autowired WorkOrderSnapshotService previews;

    private com.example.demo.dto.client.ClientSearchResponse person(String name, String phone) {
        return cards.create(new com.example.demo.dto.client.ClientCardRequest(com.example.demo.model.client.ClientType.PERSON,
                null, name, null, null, null, null, "Заметка клиента", phone == null ? List.of() : List.of(
                new com.example.demo.dto.client.ClientCardRequest.ContactInput(null, 1L, phone, "Личный")), false));
    }
    private com.example.demo.dto.client.ClientSearchResponse bicycle(com.example.demo.dto.client.ClientSearchResponse client) {
        return cards.createBicycle(client.id(), new com.example.demo.dto.client.BicycleCardRequest(null, "Cube", "Красный"));
    }
    private WorkOrderRequest request(com.example.demo.dto.client.ClientSearchResponse client, String number,
            WorkOrderRequest.SnapshotAction action, Long expectedClientVersion) {
        var bike = client.bicycles().getFirst();
        return new WorkOrderRequest(client.id(), bike.id(), client.contacts().isEmpty() ? null : client.contacts().getFirst().id(),
                "Неисправность", "Пожелание клиента", "Для себя", null, null, List.of(), "NEW", null, number,
                Instant.parse("2024-05-06T09:12:00Z"), action, expectedClientVersion, bike.version());
    }
    private com.example.demo.dto.client.ClientCardRequest rename(com.example.demo.dto.client.ClientSearchResponse client, String name) {
        return new com.example.demo.dto.client.ClientCardRequest(client.type(), client.version(), name, client.lastName(),
                client.middleName(), client.organizationName(), client.representative(), client.comment(), client.contacts().stream()
                .map(contact -> new com.example.demo.dto.client.ClientCardRequest.ContactInput(contact.id(), contact.typeId(), contact.value(), contact.label())).toList(), false);
    }

    @Test void separateCardsAreCreatedWithMinimalPersonOrOrganizationData() {
        assertNotNull(person("Иван", null).id());
        var contactOnly = person(null, "79991234567");
        assertNull(contactOnly.firstName());
        assertEquals(1, contactOnly.contacts().size());
        var organization = cards.create(new com.example.demo.dto.client.ClientCardRequest(
                com.example.demo.model.client.ClientType.ORGANIZATION, null, null, null, null, "Маяк", "Анна Орлова", "Организация", List.of(), false));
        var complete = bicycle(organization);
        var order = service.create(request(complete, "ORG", WorkOrderRequest.SnapshotAction.KEEP, complete.version()));
        assertEquals("Маяк", order.snapshot().displayName());
        assertEquals("Анна Орлова", order.snapshot().representative());
        assertEquals("ORGANIZATION", order.snapshot().clientType());
        assertEquals(3, jdbc.queryForObject("select count(*) from clients", Integer.class));
    }

    @Test void updatingClientDoesNotChangeSnapshotAndKeepSaveDoesNotEditCard() {
        var client = bicycle(person("Иван", "79991234567"));
        var saved = service.create(request(client, "SNAP", WorkOrderRequest.SnapshotAction.KEEP, client.version()));
        var updated = cards.update(client.id(), rename(client, "Пётр"));
        assertTrue(updated.version() > client.version());
        var loaded = service.findByNumber("SNAP").getFirst();
        assertEquals("Пётр", loaded.client().firstName());
        assertEquals("Иван", loaded.snapshot().displayName());
        service.update("SNAP", request(updated, "SNAP", WorkOrderRequest.SnapshotAction.KEEP, null));
        assertEquals(saved.snapshot(), service.findByNumber("SNAP").getFirst().snapshot());
        assertEquals("Пётр", search.findById(client.id()).firstName());
        assertEquals(Instant.parse("2024-05-06T09:12:00Z"), loaded.createdAt());
    }

    @Test void explicitRefreshChecksVersionAndOnlyChangesSnapshotOnSave() {
        var original = bicycle(person("Иван", "79991234567"));
        service.create(request(original, "REFRESH", WorkOrderRequest.SnapshotAction.KEEP, original.version()));
        var current = cards.update(original.id(), rename(original, "Пётр"));
        var preview = previews.preview(new com.example.demo.dto.client.ClientSelectionRequest(current.id(), current.bicycles().getFirst().id(), current.contacts().getFirst().id()));
        assertEquals("Пётр", preview.snapshot().displayName());
        assertEquals("Иван", service.findByNumber("REFRESH").getFirst().snapshot().displayName());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.update("REFRESH",
                request(original, "REFRESH", WorkOrderRequest.SnapshotAction.REFRESH, original.version()))).getStatusCode().value());
        service.update("REFRESH", request(current, "REFRESH", WorkOrderRequest.SnapshotAction.REFRESH, current.version()));
        assertEquals("Пётр", service.findByNumber("REFRESH").getFirst().snapshot().displayName());
    }

    @Test void replacementPreservesOrderFieldsAndNeverChangesPreviousClient() {
        var first = bicycle(person("Первый", "79991234567"));
        var second = bicycle(person("Второй", "78881234567"));
        service.create(request(first, "REPLACE", WorkOrderRequest.SnapshotAction.KEEP, first.version()));
        var changed = service.update("REPLACE", request(second, "REPLACE", WorkOrderRequest.SnapshotAction.KEEP, second.version()));
        assertEquals(second.id(), changed.clientId());
        assertEquals("Для себя", changed.internalComment());
        assertEquals("Неисправность", changed.reportedIssue());
        assertEquals("Первый", search.findById(first.id()).firstName());
        var invalid = new WorkOrderRequest(second.id(), first.bicycles().getFirst().id(), null, "", "", "", null, null,
                List.of(), "NEW", null, "REPLACE", null, WorkOrderRequest.SnapshotAction.REFRESH, second.version(), 0L);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.update("REPLACE", invalid)).getStatusCode().value());
    }

    @Test void staleCardEditsAndImplicitWritesThroughBlankAreRejected() {
        var client = bicycle(person("Иван", null));
        cards.update(client.id(), rename(client, "Пётр"));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> cards.update(client.id(), rename(client, "Старое изменение"))).getStatusCode().value());
        var legacy = new WorkOrderRequest(client.id(), client.bicycles().getFirst().id(), null, "", "", "", null, null,
                List.of(), "NEW", new WorkOrderDetails("Другой", "Клиент", null, "Trek", "", null, "", null, null, ""), "FAIL", null);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.create(legacy)).getStatusCode().value());
        assertEquals("Пётр", search.findById(client.id()).firstName());
        assertEquals(0, jdbc.queryForObject("select count(*) from work_orders", Integer.class));
    }

    @Test void phoneSearchSeparatesExactAndSimilarAndWarnsAboutDuplicateContacts() {
        person("Первый", "79991234567");
        person("Второй", "81191234567");
        var exact = search.search("+7 (999) 123-45-67");
        assertEquals(1, exact.exact().size());
        assertEquals(1, exact.similar().size());
        assertTrue(exact.autoSelect());
        var similar = search.search("00091234567");
        assertTrue(similar.exact().isEmpty());
        assertEquals(2, similar.similar().size());
        assertFalse(similar.autoSelect());
        var duplicate = assertThrows(com.example.demo.service.client.ClientCardConflict.class, () -> person("Третий", "+7 (999) 123-45-67"));
        assertEquals("DUPLICATE_CONTACT", duplicate.code());
        assertEquals("+7 (999) 123-45-67", duplicate.searchQuery());
        var secondContact = assertThrows(com.example.demo.service.client.ClientCardConflict.class, () -> cards.create(
                new com.example.demo.dto.client.ClientCardRequest(com.example.demo.model.client.ClientType.PERSON, null,
                        "Третий", null, null, null, null, null, List.of(
                        new com.example.demo.dto.client.ClientCardRequest.ContactInput(null, 2L, "unique@example.test", null),
                        new com.example.demo.dto.client.ClientCardRequest.ContactInput(null, 1L, "79991234567", null)), false)));
        assertEquals("79991234567", secondContact.searchQuery());
        assertEquals(2, jdbc.queryForObject("select count(*) from clients", Integer.class));
    }

    @Test void contactOnlyEditsIncrementCardVersionAndRejectForeignContacts() {
        var client = person("Иван", "79991234567");
        var other = bicycle(person("Пётр", "78881234567"));
        var contact = client.contacts().getFirst();
        var changed = cards.update(client.id(), new com.example.demo.dto.client.ClientCardRequest(client.type(), client.version(),
                client.firstName(), null, null, null, null, client.comment(), List.of(
                new com.example.demo.dto.client.ClientCardRequest.ContactInput(contact.id(), contact.typeId(), "70001112233", contact.label())), false));
        assertTrue(changed.version() > client.version());
        assertEquals("70001112233", changed.contacts().getFirst().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> cards.update(client.id(), rename(client, "Старые данные"))).getStatusCode().value());
        var invalid = new WorkOrderRequest(other.id(), other.bicycles().getFirst().id(), contact.id(), "", "", "", null, null,
                List.of(), "NEW", null, "FOREIGN", null, WorkOrderRequest.SnapshotAction.REFRESH, other.version(), other.bicycles().getFirst().version());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.create(invalid)).getStatusCode().value());
        assertEquals(0, jdbc.queryForObject("select count(*) from work_orders", Integer.class));
    }

    @Test void staleBicycleChangesAndSnapshotRefreshAreRejected() {
        var original = bicycle(person("Иван", null));
        var bike = original.bicycles().getFirst();
        service.create(request(original, "BIKE", WorkOrderRequest.SnapshotAction.KEEP, original.version()));
        var current = cards.updateBicycle(original.id(), bike.id(), new com.example.demo.dto.client.BicycleCardRequest(bike.version(), "Trek", "Синий"));
        assertTrue(current.bicycles().getFirst().version() > bike.version());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> cards.updateBicycle(original.id(), bike.id(),
                new com.example.demo.dto.client.BicycleCardRequest(bike.version(), "Старое изменение", ""))).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.update("BIKE",
                request(original, "BIKE", WorkOrderRequest.SnapshotAction.REFRESH, original.version()))).getStatusCode().value());
        assertEquals("Cube", service.findByNumber("BIKE").getFirst().snapshot().bicycleBrand());
        service.update("BIKE", request(current, "BIKE", WorkOrderRequest.SnapshotAction.REFRESH, current.version()));
        assertEquals("Trek", service.findByNumber("BIKE").getFirst().snapshot().bicycleBrand());
    }

    @Test void invalidMinimalCardsAndTypeChangesAreRejectedAndSharedContactRequiresConfirmation() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> person(null, null)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> cards.create(new com.example.demo.dto.client.ClientCardRequest(
                com.example.demo.model.client.ClientType.ORGANIZATION, null, null, null, null, " ", null, null, List.of(), false))).getStatusCode().value());
        var person = person("Иван", "79991234567");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> cards.update(person.id(), new com.example.demo.dto.client.ClientCardRequest(
                com.example.demo.model.client.ClientType.ORGANIZATION, person.version(), null, null, null, "Компания", null, null, List.of(), false))).getStatusCode().value());
        cards.create(new com.example.demo.dto.client.ClientCardRequest(com.example.demo.model.client.ClientType.PERSON, null,
                "Пётр", null, null, null, null, null, List.of(new com.example.demo.dto.client.ClientCardRequest.ContactInput(null, 1L, "+7 (999) 123-45-67", null)), true));
        assertEquals(2, search.search("79991234567").exact().size());
        assertFalse(search.search("79991234567").autoSelect());
    }
}
