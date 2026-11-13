package com.example.demo.service.catalog;

import com.example.demo.dto.service.CreateServicePriceRequest;
import com.example.demo.model.service.ServicePrice;
import com.example.demo.model.service.WorkshopService;
import com.example.demo.repository.ServicePriceRepository;
import com.example.demo.repository.WorkshopServiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicePriceServiceTests {
    private final WorkshopServiceRepository services = mock(WorkshopServiceRepository.class);
    private final ServicePriceRepository prices = mock(ServicePriceRepository.class);
    private final ServicePriceServiceImpl catalog = new ServicePriceServiceImpl(services, prices);
    private final WorkshopService service = new WorkshopService("Услуга", null);

    @BeforeEach
    void setup() {
        service.setId(1L);
        when(services.findByIdForPriceUpdate(1L)).thenReturn(Optional.of(service));
        when(prices.save(any())).thenAnswer(call -> {
            ServicePrice price = call.getArgument(0);
            price.setId(2L);
            return price;
        });
    }

    @Test
    void closesPreviousPriceBeforeNewOneStarts() {
        ServicePrice previous = new ServicePrice(service, BigDecimal.TEN, LocalDate.of(2026, 1, 1));
        when(prices.findFirstByServiceIdOrderByValidFromDescIdDesc(1L)).thenReturn(Optional.of(previous));
        var result = catalog.create(1L, new CreateServicePriceRequest(new BigDecimal("25.00"), LocalDate.of(2026, 2, 1)));
        assertEquals(LocalDate.of(2026, 1, 31), previous.getValidTo());
        assertEquals(LocalDate.of(2026, 2, 1), result.validFrom());
        assertEquals(new BigDecimal("25.00"), result.price());
        assertNull(result.validTo());
        assertEquals(1L, result.serviceId());
        verify(services).findByIdForPriceUpdate(1L);
    }

    @Test
    void rejectsSameOrEarlierDateWithoutChangingHistory() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        ServicePrice previous = new ServicePrice(service, BigDecimal.TEN, start);
        when(prices.findFirstByServiceIdOrderByValidFromDescIdDesc(1L)).thenReturn(Optional.of(previous));
        for (LocalDate date : new LocalDate[]{start, start.minusDays(1)}) {
            var error = assertThrows(ResponseStatusException.class,
                    () -> catalog.create(1L, new CreateServicePriceRequest(BigDecimal.ONE, date)));
            assertEquals(409, error.getStatusCode().value());
            assertNull(previous.getValidTo());
        }
        verify(prices, never()).save(any());
    }

    @Test
    void doesNotExtendPreviouslyClosedPeriod() {
        ServicePrice previous = new ServicePrice(service, BigDecimal.TEN, LocalDate.of(2026, 1, 1));
        previous.setValidTo(LocalDate.of(2026, 1, 10));
        when(prices.findFirstByServiceIdOrderByValidFromDescIdDesc(1L)).thenReturn(Optional.of(previous));
        catalog.create(1L, new CreateServicePriceRequest(BigDecimal.ONE, LocalDate.of(2026, 2, 1)));
        assertEquals(LocalDate.of(2026, 1, 10), previous.getValidTo());
    }

    @Test
    void missingServiceReturns404() {
        var error = assertThrows(ResponseStatusException.class,
                () -> catalog.create(999L, new CreateServicePriceRequest(BigDecimal.ONE, null)));
        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(prices);
    }

    @Test
    void firstPriceDefaultsToToday() {
        LocalDate before = LocalDate.now();
        var result = catalog.create(1L, new CreateServicePriceRequest(BigDecimal.ZERO, null));
        assertFalse(result.validFrom().isBefore(before));
        assertFalse(result.validFrom().isAfter(LocalDate.now()));
    }
}
