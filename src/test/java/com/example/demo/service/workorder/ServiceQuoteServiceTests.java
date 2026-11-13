package com.example.demo.service.workorder;

import com.example.demo.dto.workorder.ServiceQuoteLineRequest;
import com.example.demo.dto.workorder.ServiceQuoteRequest;
import com.example.demo.model.service.Discount;
import com.example.demo.model.service.ServicePrice;
import com.example.demo.model.service.WorkshopService;
import com.example.demo.repository.DiscountRepository;
import com.example.demo.repository.ServicePriceRepository;
import com.example.demo.repository.WorkshopServiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServiceQuoteServiceTests {
    private final WorkshopServiceRepository services = mock(WorkshopServiceRepository.class);
    private final ServicePriceRepository prices = mock(ServicePriceRepository.class);
    private final DiscountRepository discounts = mock(DiscountRepository.class);
    private final ServiceQuoteServiceImpl quotes = new ServiceQuoteServiceImpl(services, prices, discounts);

    private void catalog(String amount, String percentage) {
        WorkshopService service = new WorkshopService("Ремонт", null);
        service.setId(1L);
        when(services.findById(1L)).thenReturn(Optional.of(service));
        when(prices.findActivePrices(eq(1L), any(), any())).thenReturn(List.of(
                new ServicePrice(service, new BigDecimal(amount), LocalDate.now())));
        Discount discount = new Discount("Клиент", new BigDecimal(percentage));
        discount.setId(7L);
        when(discounts.findById(7L)).thenReturn(Optional.of(discount));
    }

    @Test
    void calculatesDiscountsAndTotalsAndKeepsDuplicateServices() {
        catalog("10.05", "10");
        var result = quotes.calculate(new ServiceQuoteRequest(List.of(
                new ServiceQuoteLineRequest(1L, 7L, null, null, null),
                new ServiceQuoteLineRequest(1L, null, null, null, null))));
        assertEquals(2, result.serviceCount());
        assertEquals(new BigDecimal("20.10"), result.totalPrice());
        assertEquals(new BigDecimal("19.10"), result.totalWithDiscount());
        assertEquals(new BigDecimal("9.05"), result.services().getFirst().priceWithDiscount());
        assertEquals("Клиент", result.services().getFirst().discountName());
    }

    @Test
    void preservesStoredPricesAndNameWhenLoadingOldBlank() {
        catalog("999.00", "10");
        var result = quotes.calculate(new ServiceQuoteRequest(List.of(new ServiceQuoteLineRequest(
                1L, 7L, new BigDecimal("100.00"), new BigDecimal("87.00"), "Старое название"))));
        assertEquals(new BigDecimal("100.00"), result.totalPrice());
        assertEquals(new BigDecimal("87.00"), result.totalWithDiscount());
        assertEquals("Старое название", result.services().getFirst().serviceNameSnapshot());
        verifyNoInteractions(prices);
    }

    @Test
    void zeroAndFullDiscountAreSupported() {
        for (String percentage : List.of("0", "100")) {
            catalog("123.45", percentage);
            var result = quotes.calculate(new ServiceQuoteRequest(List.of(
                    new ServiceQuoteLineRequest(1L, 7L, null, null, null))));
            assertEquals(new BigDecimal(percentage.equals("0") ? "123.45" : "0.00"), result.totalWithDiscount());
        }
    }

    @Test
    void emptyBlankHasZeroTotals() {
        var result = quotes.calculate(new ServiceQuoteRequest(List.of()));
        assertEquals(0, result.serviceCount());
        assertEquals(new BigDecimal("0.00"), result.totalWithDiscount());
        verifyNoInteractions(services, prices, discounts);
    }

    @Test
    void rejectsMissingServicePriceAndDiscount() {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> quotes.calculate(
                new ServiceQuoteRequest(List.of(new ServiceQuoteLineRequest(99L, null, null, null, null)))))
                .getStatusCode().value());
        catalog("100", "10");
        when(prices.findActivePrices(eq(1L), any(), any())).thenReturn(List.of());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> quotes.calculate(
                new ServiceQuoteRequest(List.of(new ServiceQuoteLineRequest(1L, null, null, null, null)))))
                .getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> quotes.calculate(
                new ServiceQuoteRequest(List.of(new ServiceQuoteLineRequest(1L, 99L, BigDecimal.TEN, null, null)))))
                .getStatusCode().value());
    }
}
