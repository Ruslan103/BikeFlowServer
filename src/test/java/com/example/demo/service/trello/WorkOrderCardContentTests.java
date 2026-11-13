package com.example.demo.service.trello;

import com.example.demo.model.client.Client;
import com.example.demo.model.workorder.WorkOrder;
import com.example.demo.model.workorder.WorkOrderServiceItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkOrderCardContentTests {
    @Test
    void usesSavedSnapshotInsteadOfCurrentDirectoryCard() {
        Client client = mock(Client.class);
        when(client.getFirstName()).thenReturn("Новое имя");
        WorkOrder order = new WorkOrder();
        order.setClient(client);
        order.setBlankNumber("SNAPSHOT");
        order.setContactValue("Новый контакт");
        order.setClientSnapshot(new com.example.demo.model.workorder.WorkOrderClientSnapshot("ORGANIZATION", "Сохранённая организация",
                null, null, null, "Сохранённая организация", "Сохранённый представитель", "PHONE", "Сохранённый контакт", "Cube", "Красный", null));
        String text = WorkOrderCardContent.description(order, List.of());
        assertTrue(WorkOrderCardContent.title(order).contains("Сохранённая организация"));
        for (String expected : List.of("Сохранённая организация", "Сохранённый представитель", "Сохранённый контакт", "Cube", "Красный")) {
            assertTrue(text.contains(expected));
        }
        assertFalse(text.contains("Новое имя"));
        assertFalse(text.contains("Новый контакт"));
    }

    @Test
    void includesRequiredBlankFieldsAndStableMarker() {
        Client client = mock(Client.class);
        when(client.getLastName()).thenReturn("Иванов");
        when(client.getFirstName()).thenReturn("Иван");
        WorkOrder order = new WorkOrder();
        order.setClient(client);
        order.setBlankNumber("00001");
        order.setCreatedAt(Instant.parse("2026-11-07T10:00:00Z"));
        order.setContactValue("example@example.test");
        order.setInternalComment("Комментарий для себя");
        order.setReportedIssue("Не работают тормоза");
        WorkOrderServiceItem item = new WorkOrderServiceItem();
        item.setServiceNameSnapshot("Настройка тормозов");
        item.setPriceWithoutDiscount(new BigDecimal("100.00"));
        item.setPriceWithDiscount(new BigDecimal("90.00"));
        String description = WorkOrderCardContent.description(order, List.of(item));
        for (String expected : List.of("Иванов Иван", "example@example.test", "Комментарий для себя",
                "Не работают тормоза", "Настройка тормозов", "100.00", "90.00", WorkOrderCardContent.marker(order))) {
            assertTrue(description.contains(expected));
        }
        assertTrue(WorkOrderCardContent.title(order).contains("00001"));
    }
}
