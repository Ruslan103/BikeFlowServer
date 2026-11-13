package com.example.demo.controller;

import com.example.demo.dto.trello.TrelloMemberResponse;
import com.example.demo.dto.trello.WorkOrderTrelloResponse;
import com.example.demo.service.trello.TrelloCatalogService;
import com.example.demo.service.trello.WorkOrderTrelloService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TrelloControllerTests {
    private final TrelloCatalogService catalog = mock(TrelloCatalogService.class);
    private final WorkOrderTrelloService orders = mock(WorkOrderTrelloService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TrelloController(catalog), new WorkOrderTrelloController(orders))
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void readsConnectionAndBoards() throws Exception {
        when(catalog.connection()).thenReturn(new TrelloMemberResponse("1", "bike", "Bike"));
        when(catalog.boards()).thenReturn(List.of());
        mvc.perform(get("/api/trello/connection")).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("bike"));
        mvc.perform(get("/api/trello/boards")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void linksAndCreatesCard() throws Exception {
        var response = new WorkOrderTrelloResponse("00001", "card", "https://trello.com/c/short123", "LINKED");
        when(orders.link(eq("00001"), any())).thenReturn(response);
        when(orders.createCard(eq("00001"), any())).thenReturn(response);
        mvc.perform(put("/api/work-orders/00001/trello").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardId\":\"aaaaaaaaaaaaaaaaaaaaaaaa\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LINKED"));
        mvc.perform(post("/api/work-orders/00001/trello/card").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listId\":\"bbbbbbbbbbbbbbbbbbbbbbbb\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cardId").value("card"));
    }

    @Test
    void rejectsInvalidIdsAndNullLabels() throws Exception {
        mvc.perform(put("/api/work-orders/00001/trello").contentType(MediaType.APPLICATION_JSON).content("{\"cardId\":\"../bad\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/work-orders/00001/trello/card").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listId\":\"bbbbbbbbbbbbbbbbbbbbbbbb\",\"labelIds\":[null]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orders);
    }
}
