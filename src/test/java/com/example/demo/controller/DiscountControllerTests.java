package com.example.demo.controller;

import com.example.demo.dto.discount.DiscountResponse;
import com.example.demo.service.discount.DiscountService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DiscountControllerTests {
    private final DiscountService discounts = mock(DiscountService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DiscountController(discounts))
            .setControllerAdvice(new ApiExceptionHandler()).build();
    private final DiscountResponse discount = new DiscountResponse(7L, "Постоянный клиент", new BigDecimal("10.50"));

    @Test
    void createsWithLocation() throws Exception {
        when(discounts.create(any())).thenReturn(discount);
        mvc.perform(post("/api/discounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Постоянный клиент\",\"percentage\":10.50}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/discounts/7"))
                .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.percentage").value(10.5));
    }

    @Test
    void listsAndFindsDiscounts() throws Exception {
        when(discounts.findAll()).thenReturn(List.of(discount));
        when(discounts.findById(7L)).thenReturn(discount);
        mvc.perform(get("/api/discounts")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7));
        mvc.perform(get("/api/discounts/7")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value(discount.name()));
    }

    @Test
    void updatesAndDeletes() throws Exception {
        when(discounts.update(eq(7L), any())).thenReturn(discount);
        mvc.perform(put("/api/discounts/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Постоянный клиент\",\"percentage\":10.50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        mvc.perform(delete("/api/discounts/7")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(discounts).delete(7L);
    }

    @Test
    void rejectsInvalidInputForBothWrites() throws Exception {
        for (String body : new String[]{
                "{\"percentage\":10}", "{\"name\":\"   \",\"percentage\":10}",
                "{\"name\":\"Скидка\"}", "{\"name\":\"Скидка\",\"percentage\":-0.01}",
                "{\"name\":\"Скидка\",\"percentage\":100.01}", "{\"name\":\"Скидка\",\"percentage\":1.123}",
                "{\"name\":\"" + "x".repeat(101) + "\",\"percentage\":10}"}) {
            mvc.perform(post("/api/discounts").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/discounts/7").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(discounts);
    }

    @Test
    void acceptsBoundaryPercentages() throws Exception {
        when(discounts.create(any())).thenReturn(discount);
        for (String percentage : new String[]{"0", "100.00"}) {
            mvc.perform(post("/api/discounts").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Скидка\",\"percentage\":" + percentage + "}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void returnsNotFoundAndConflict() throws Exception {
        when(discounts.findById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Discount not found: 99"));
        doThrow(new DataIntegrityViolationException("foreign key")).when(discounts).delete(7L);
        mvc.perform(get("/api/discounts/99")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/discounts/7")).andExpect(status().isConflict());
    }
}
