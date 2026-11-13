package com.example.demo.controller;

import com.example.demo.dto.workorder.ServiceQuoteResponse;
import com.example.demo.service.workorder.ServiceQuoteService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ServiceQuoteControllerTests {
    private final ServiceQuoteService quotes = mock(ServiceQuoteService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ServiceQuoteController(quotes))
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void returnsCalculatedSummary() throws Exception {
        when(quotes.calculate(any())).thenReturn(new ServiceQuoteResponse(List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO));
        mvc.perform(post("/api/work-orders/services/quote").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"services\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serviceCount").value(0));
    }

    @Test
    void validatesNestedRows() throws Exception {
        for (String body : List.of("{}", "{\"services\":[null]}", "{\"services\":[{}]}",
                "{\"services\":[{\"serviceId\":1,\"priceWithoutDiscount\":-1}]}",
                "{\"services\":[{\"serviceId\":1,\"priceWithDiscount\":1.123}]}")) {
            mvc.perform(post("/api/work-orders/services/quote").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(quotes);
    }
}
