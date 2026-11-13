package com.example.demo.controller;

import com.example.demo.dto.service.ServicePriceResponse;
import com.example.demo.service.catalog.ServicePriceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ServicePriceControllerTests {
    private final ServicePriceService prices = mock(ServicePriceService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ServicePriceController(prices))
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void createsPriceWithLocation() throws Exception {
        when(prices.create(eq(1L), any())).thenReturn(new ServicePriceResponse(2L, 1L,
                new BigDecimal("500.00"), LocalDate.of(2026, 12, 1), null));
        mvc.perform(post("/api/services/1/prices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":500.00,\"validFrom\":\"2026-12-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/services/1/prices/2"))
                .andExpect(jsonPath("$.serviceId").value(1));
    }

    @Test
    void rejectsInvalidPrices() throws Exception {
        for (String body : new String[]{"{}", "{\"price\":-1}", "{\"price\":1.123}", "{\"price\":10000000000}"}) {
            mvc.perform(post("/api/services/1/prices").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(prices);
    }

    @Test
    void rejectsPriceBelongingToAnotherService() throws Exception {
        when(prices.findById(1L, 99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        mvc.perform(get("/api/services/1/prices/99")).andExpect(status().isNotFound());
    }
}
