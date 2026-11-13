package com.example.demo.service.trello;

import com.example.demo.dto.trello.CreateWorkOrderCardRequest;
import com.example.demo.dto.trello.LinkTrelloCardRequest;
import com.example.demo.dto.trello.TrelloCardResponse;
import com.example.demo.dto.trello.TrelloListResponse;
import com.example.demo.integration.trello.TrelloClient;
import com.example.demo.model.workorder.TrelloSyncStatus;
import com.example.demo.model.workorder.WorkOrder;
import com.example.demo.repository.TrelloSyncStatusRepository;
import com.example.demo.repository.WorkOrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkOrderTrelloServiceTests {
    private static final String CARD = "aaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String LIST = "bbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String BOARD = "cccccccccccccccccccccccc";
    private final WorkOrderRepository orders = mock(WorkOrderRepository.class);
    private final TrelloSyncStatusRepository statuses = mock(TrelloSyncStatusRepository.class);
    private final TrelloClient trello = mock(TrelloClient.class);
    private final com.example.demo.repository.WorkOrderServiceItemRepository items = mock(com.example.demo.repository.WorkOrderServiceItemRepository.class);
    private final WorkOrderTrelloService service = new WorkOrderTrelloServiceImpl(orders, statuses, trello, items,
            mock(com.example.demo.repository.WorkOrderProductItemRepository.class),
            mock(com.example.demo.repository.WorkOrderCustomerPartRepository.class));

    private WorkOrder order() {
        WorkOrder order = new WorkOrder();
        order.setId(7L);
        order.setBlankNumber("00001");
        order.setCreatedAt(Instant.parse("2026-11-07T10:00:00Z"));
        when(orders.findByNumberForUpdate("00001")).thenReturn(Optional.of(order));
        TrelloSyncStatus status = mock(TrelloSyncStatus.class);
        when(status.getCode()).thenReturn("LINKED");
        when(statuses.findByCode("LINKED")).thenReturn(Optional.of(status));
        when(trello.list(LIST)).thenReturn(new TrelloListResponse(LIST, "Новые", BOARD, false));
        when(trello.boardCards(BOARD)).thenReturn(List.of());
        when(trello.card(CARD)).thenReturn(card(null));
        when(trello.updateCard(anyString(), anyString(), anyString(), anyString(), anyString(), anyList())).thenReturn(card(null));
        return order;
    }
    private TrelloCardResponse card(String description) {
        return new TrelloCardResponse(CARD, "Карточка", "https://trello.com/c/short123", BOARD, LIST, false, description);
    }

    @Test
    void linksExistingCardAndRepeatedCallIsIdempotent() {
        WorkOrder order = order();
        when(trello.card(CARD)).thenReturn(card(null));
        var response = service.link("00001", new LinkTrelloCardRequest(CARD));
        assertEquals(CARD, order.getTrelloCard());
        assertEquals("LINKED", response.status());
        assertEquals(response, service.link("00001", new LinkTrelloCardRequest(CARD)));
        verify(trello, times(1)).card(CARD);
        verify(orders).flush();
    }

    @Test
    void doesNotReplaceLinkOrReuseAnotherOrdersCard() {
        WorkOrder order = order();
        order.setTrelloCard("dddddddddddddddddddddddd");
        when(trello.card(CARD)).thenReturn(card(null));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.link("00001", new LinkTrelloCardRequest(CARD))).getStatusCode().value());
        order.setTrelloCard(null);
        when(orders.existsByTrelloCardAndIdNot(CARD, 7L)).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.link("00001", new LinkTrelloCardRequest(CARD))).getStatusCode().value());
        assertNull(order.getTrelloCard());
    }

    @Test
    void createsCardOnceAndStoresLink() {
        order();
        when(trello.createCard(eq(LIST), anyString(), anyString(), eq(List.of()))).thenReturn(card(null));
        var request = new CreateWorkOrderCardRequest(LIST, null);
        var first = service.createCard("00001", request);
        assertEquals(CARD, first.cardId());
        assertEquals(first, service.createCard("00001", request));
        verify(trello).createCard(eq(LIST), startsWith("Бланк № 00001"), contains("[bike-work-order:00001:2026-11-07T10:00:00Z]"), eq(List.of()));
    }

    @Test
    void recoversCardAfterUnknownCreationOutcomeWithoutCreatingDuplicate() {
        order();
        when(trello.boardCards(BOARD)).thenReturn(List.of(card("description\n[bike-work-order:00001:2026-11-07T10:00:00Z]")));
        assertEquals(CARD, service.createCard("00001", new CreateWorkOrderCardRequest(LIST, null)).cardId());
        verify(trello, never()).createCard(any(), any(), any(), any());
    }

    @Test
    void failureLeavesOrderUnlinkedAndForeignLabelIsRejected() {
        WorkOrder order = order();
        when(trello.labels(BOARD)).thenReturn(List.of());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.createCard("00001",
                new CreateWorkOrderCardRequest(LIST, List.of(CARD)))).getStatusCode().value());
        when(trello.createCard(any(), any(), any(), any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY));
        assertThrows(ResponseStatusException.class, () -> service.createCard("00001", new CreateWorkOrderCardRequest(LIST, null)));
        assertNull(order.getTrelloCard());
        verify(orders, never()).flush();
    }

    @Test
    void archivedCardIsNotModified() {
        WorkOrder order = order();
        order.setTrelloCard(CARD);
        when(trello.card(CARD)).thenReturn(new TrelloCardResponse(CARD, "Archived",
                "https://trello.com/c/short123", BOARD, LIST, true, null));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.createCard("00001",
                new CreateWorkOrderCardRequest(LIST, null))).getStatusCode().value());
        verify(trello, never()).updateCard(any(), any(), any(), any(), any(), any());
    }

    @Test
    void missingOrderAndMissingStatusDoNotCreateExternalCard() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.link("missing", new LinkTrelloCardRequest(CARD))).getStatusCode().value());
        order();
        when(statuses.findByCode("LINKED")).thenReturn(Optional.empty());
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> service.createCard("00001", new CreateWorkOrderCardRequest(LIST, null))).getStatusCode().value());
        verifyNoInteractions(trello);
    }
}
