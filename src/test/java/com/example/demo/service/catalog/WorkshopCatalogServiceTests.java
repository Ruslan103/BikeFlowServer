package com.example.demo.service.catalog;

import com.example.demo.dto.service.CreateWorkshopServiceRequest;
import com.example.demo.model.service.ServicePrice;
import com.example.demo.model.service.WorkshopService;
import com.example.demo.repository.ServicePriceRepository;
import com.example.demo.repository.WorkshopServiceRepository;
import com.example.demo.repository.WorkOrderServiceItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkshopCatalogServiceTests {
    private final WorkshopServiceRepository services = mock(WorkshopServiceRepository.class);
    private final ServicePriceRepository prices = mock(ServicePriceRepository.class);
    private final WorkOrderServiceItemRepository workOrderServiceItems = mock(WorkOrderServiceItemRepository.class);
    private final WorkshopCatalogServiceImpl catalog =
            new WorkshopCatalogServiceImpl(services, prices, workOrderServiceItems);

    @Test
    void createsServiceAndInitialPrice() {
        when(services.save(any())).thenAnswer(call -> {
            WorkshopService service = call.getArgument(0);
            service.setId(7L);
            return service;
        });
        when(prices.save(any())).thenAnswer(call -> call.getArgument(0));
        LocalDate date = LocalDate.of(2026, 1, 15);
        var result = catalog.create(new CreateWorkshopServiceRequest("  Настройка тормозов  ", "Описание",
                new BigDecimal("450.00"), date));

        var price = ArgumentCaptor.forClass(ServicePrice.class);
        verify(prices).save(price.capture());
        assertEquals(7L, result.id());
        assertEquals("Настройка тормозов", result.name());
        assertEquals(new BigDecimal("450.00"), result.price());
        assertEquals(date, result.validFrom());
        assertEquals(7L, price.getValue().getService().getId());
        assertNull(price.getValue().getValidTo());
    }

    @Test
    void defaultsPriceStartToToday() {
        when(services.save(any())).thenAnswer(call -> call.getArgument(0));
        when(prices.save(any())).thenAnswer(call -> call.getArgument(0));
        LocalDate before = LocalDate.now();
        var result = catalog.create(new CreateWorkshopServiceRequest("Услуга", null, BigDecimal.ZERO, null));
        assertFalse(result.validFrom().isBefore(before));
        assertFalse(result.validFrom().isAfter(LocalDate.now()));
    }

    @Test
    void missingServiceReturns404() {
        when(services.findById(99L)).thenReturn(Optional.empty());
        var exception = assertThrows(ResponseStatusException.class, () -> catalog.findById(99L));
        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(prices);
    }

    @Test
    void serviceWithNoCurrentPriceRemainsVisible() {
        WorkshopService service = new WorkshopService("Услуга", null);
        service.setId(8L);
        when(services.findById(8L)).thenReturn(Optional.of(service));
        when(prices.findActivePrices(eq(8L), any(), any())).thenReturn(List.of());
        var response = catalog.findById(8L);
        assertEquals(8L, response.id());
        assertNull(response.price());
    }
}
