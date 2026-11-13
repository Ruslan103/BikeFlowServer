package com.example.demo.controller;

import com.example.demo.dto.client.ClientSearchResponse;
import com.example.demo.service.client.ClientSearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientSearchControllerTests {
    private final ClientSearchService clients = mock(ClientSearchService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ClientSearchController(clients))
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void returnsClientsWithNestedCollections() throws Exception {
        when(clients.findByPhone("79991234567")).thenReturn(List.of(new ClientSearchResponse(
                1L, 100L, "Иванов", "Иван", null, "Комментарий", List.of(), List.of())));
        mvc.perform(get("/api/clients/search").param("phone", "79991234567"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].comment").value("Комментарий"))
                .andExpect(jsonPath("$[0].bicycles").isArray()).andExpect(jsonPath("$[0].contacts").isArray());
    }

    @Test
    void missingPhoneReturns400AndNoMatchesReturnsEmptyArray() throws Exception {
        when(clients.findByPhone(null)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone"));
        when(clients.findByPhone("79990000000")).thenReturn(List.of());
        mvc.perform(get("/api/clients/search")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/clients/search").param("phone", "79990000000"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
