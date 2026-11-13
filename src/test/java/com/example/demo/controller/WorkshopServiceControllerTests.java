package com.example.demo.controller;

import com.example.demo.dto.service.WorkshopServiceResponse;
import com.example.demo.service.catalog.WorkshopCatalogService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkshopServiceControllerTests {
    private final WorkshopCatalogService catalog = mock(WorkshopCatalogService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new WorkshopServiceController(catalog))
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void createsServiceWithLocation() throws Exception {
        when(catalog.create(any())).thenReturn(new WorkshopServiceResponse(7L, "Тормоза", null,
                new BigDecimal("450.00"), LocalDate.of(2026, 1, 15), null));
        mvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Тормоза","price":450.00,"validFrom":"2026-01-15"}
                                """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/services/7"))
                .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.price").value(450.0));
    }

    @Test
    void rejectsInvalidNamesAndPrices() throws Exception {
        for (String body : new String[]{
                "{\"name\":\" \",\"price\":1}",
                "{\"name\":\"Услуга\",\"price\":-1}",
                "{\"name\":\"Услуга\"}",
                "{\"name\":\"Услуга\",\"price\":1.123}",
                "{\"name\":\"Услуга\",\"price\":10000000000}",
                "{\"name\":\"" + "x".repeat(151) + "\",\"price\":1}"}) {
            mvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(catalog);
    }
}
