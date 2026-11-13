package com.example.demo.service.discount;

import com.example.demo.dto.discount.DiscountRequest;
import com.example.demo.model.service.Discount;
import com.example.demo.repository.DiscountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DiscountServiceTests {
    private final DiscountRepository repository = mock(DiscountRepository.class);
    private final DiscountServiceImpl service = new DiscountServiceImpl(repository);

    @Test
    void createsTrimmedDiscount() {
        when(repository.save(any())).thenAnswer(call -> {
            Discount discount = call.getArgument(0);
            discount.setId(7L);
            return discount;
        });
        var result = service.create(new DiscountRequest("  Клиент  ", new BigDecimal("12.50")));
        assertEquals(7L, result.id());
        assertEquals("Клиент", result.name());
        assertEquals(new BigDecimal("12.50"), result.percentage());
    }

    @Test
    void listsInStableNameOrderAndFindsById() {
        Discount discount = discount();
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(discount));
        when(repository.findById(7L)).thenReturn(Optional.of(discount));
        assertEquals(service.findById(7L), service.findAll().getFirst());
        verify(repository).findAll(Sort.by("name").and(Sort.by("id")));
    }

    @Test
    void updatesExistingEntityPreservingId() {
        Discount discount = discount();
        when(repository.findById(7L)).thenReturn(Optional.of(discount));
        var result = service.update(7L, new DiscountRequest("  Новое  ", BigDecimal.ZERO));
        assertEquals(7L, result.id());
        assertEquals("Новое", discount.getName());
        assertEquals(BigDecimal.ZERO, discount.getPercentage());
    }

    @Test
    void missingDiscountReturns404ForReadUpdateAndDelete() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.findById(99L)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.update(99L, new DiscountRequest("Скидка", BigDecimal.TEN))).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.delete(99L)).getStatusCode().value());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletesAndFlushesConstraints() {
        Discount discount = discount();
        when(repository.findById(7L)).thenReturn(Optional.of(discount));
        service.delete(7L);
        var order = inOrder(repository);
        order.verify(repository).delete(discount);
        order.verify(repository).flush();
    }

    @Test
    void referencedDiscountPropagatesIntegrityConflict() {
        when(repository.findById(7L)).thenReturn(Optional.of(discount()));
        doThrow(new DataIntegrityViolationException("foreign key")).when(repository).flush();
        assertThrows(DataIntegrityViolationException.class, () -> service.delete(7L));
    }

    private Discount discount() {
        Discount discount = new Discount("Клиент", BigDecimal.TEN);
        discount.setId(7L);
        return discount;
    }
}
