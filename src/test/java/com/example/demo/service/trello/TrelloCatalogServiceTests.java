package com.example.demo.service.trello;

import com.example.demo.integration.trello.TrelloClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrelloCatalogServiceTests {
    @Test
    void invalidPathsNeverReachExternalClient() {
        TrelloClient client = mock(TrelloClient.class);
        TrelloCatalogService service = new TrelloCatalogServiceImpl(client);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.lists("../members/me")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.labels("?key=bad")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.cards(null)).getStatusCode().value());
        verifyNoInteractions(client);
    }
}
