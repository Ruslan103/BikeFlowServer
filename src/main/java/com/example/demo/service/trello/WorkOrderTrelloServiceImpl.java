package com.example.demo.service.trello;

import com.example.demo.dto.trello.CreateWorkOrderCardRequest;
import com.example.demo.dto.trello.LinkTrelloCardRequest;
import com.example.demo.dto.trello.TrelloCardResponse;
import com.example.demo.dto.trello.TrelloLabelResponse;
import com.example.demo.dto.trello.WorkOrderTrelloResponse;
import com.example.demo.integration.trello.TrelloClient;
import com.example.demo.model.workorder.WorkOrder;
import com.example.demo.repository.TrelloSyncStatusRepository;
import com.example.demo.repository.WorkOrderRepository;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WorkOrderTrelloServiceImpl implements WorkOrderTrelloService {
    private final WorkOrderRepository orders;
    private final TrelloSyncStatusRepository statuses;
    private final TrelloClient trello;
    private final com.example.demo.repository.WorkOrderServiceItemRepository items;
    private final com.example.demo.repository.WorkOrderProductItemRepository productItems;
    private final com.example.demo.repository.WorkOrderCustomerPartRepository customerParts;

    @Override
    @Transactional(readOnly = true)
    public TrelloCardResponse readCard(String number) {
        WorkOrder order = orders.findByBlankNumber(number).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Бланк не найден."));
        if (order.getTrelloCard() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Карточка не привязана.");
        return trello.card(order.getTrelloCard());
    }

    @Override
    @Transactional
    public WorkOrderTrelloResponse link(String number, LinkTrelloCardRequest request) {
        WorkOrder order = requireOrder(number);
        if (Objects.equals(order.getTrelloCard(), request.cardId())) return response(order);
        TrelloCardResponse card = trello.card(request.cardId());
        if (Objects.equals(order.getTrelloCard(), card.id())) return response(order);
        if (order.getTrelloCard() != null) throw conflict("Бланк уже привязан к другой карточке Trello.");
        return attach(order, card);
    }

    @Override
    @Transactional
    public WorkOrderTrelloResponse createCard(String number, CreateWorkOrderCardRequest request) {
        WorkOrder order = requireOrder(number);
        // Проверяем локальные зависимости до внешней операции.
        requireLinkedStatus();
        var list = trello.list(request.listId());
        if (list.closed()) throw conflict("Выбранная колонка Trello закрыта.");
        String marker = WorkOrderCardContent.marker(order);
        validateLabels(request, list.idBoard());
        String title = WorkOrderCardContent.title(order);
        String description = WorkOrderCardContent.description(order,
                items.findAllByWorkOrderIdOrderByIdAsc(order.getId()),
                productItems.findAllByWorkOrderIdOrderByIdAsc(order.getId()),
                customerParts.findAllByWorkOrderIdOrderByIdAsc(order.getId()));
        if (order.getTrelloCard() != null) {
            if (trello.card(order.getTrelloCard()).closed()) throw conflict("Карточка Trello архивирована.");
            return attach(order, trello.updateCard(order.getTrelloCard(), list.idBoard(), list.id(), title, description, request.labelIds()));
        }
        List<TrelloCardResponse> recovered = trello.boardCards(list.idBoard()).stream()
                .filter(card -> card.desc() != null && card.desc().lines().anyMatch(marker::equals)).toList();
        if (recovered.size() > 1) throw conflict("На доске найдено несколько карточек этого бланка. Выберите карточку вручную.");
        if (!recovered.isEmpty()) {
            var existing = recovered.getFirst();
            if (existing.closed()) throw conflict("Карточка Trello архивирована.");
            return attach(order, trello.updateCard(existing.id(), list.idBoard(), list.id(), title, description, request.labelIds()));
        }
        var card = trello.createCard(list.id(), title, description, request.labelIds());
        return attach(order, card);
    }

    private void validateLabels(CreateWorkOrderCardRequest request, String boardId) {
        if (!request.labelIds().isEmpty()) {
            var allowed = trello.labels(boardId).stream().map(TrelloLabelResponse::id).toList();
            if (!allowed.containsAll(request.labelIds())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выбрана метка с другой доски Trello.");
            }
        }
    }

    private WorkOrder requireOrder(String number) {
        return orders.findByNumberForUpdate(number).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Бланк не найден."));
    }

    private com.example.demo.model.workorder.TrelloSyncStatus requireLinkedStatus() {
        return statuses.findByCode("LINKED").orElseThrow(() -> new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Не настроен статус LINKED. Обновите схему базы и перезапустите сервер."));
    }

    private WorkOrderTrelloResponse attach(WorkOrder order, TrelloCardResponse card) {
        if (card == null || card.id() == null || !card.id().matches("[a-fA-F0-9]{24}") || !validUrl(card.url())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Trello вернул некорректную карточку.");
        }
        if (card.closed()) throw conflict("Карточка Trello архивирована.");
        if (orders.existsByTrelloCardAndIdNot(card.id(), order.getId())) {
            throw conflict("Карточка Trello уже связана с другим бланком.");
        }
        order.setTrelloSyncStatus(requireLinkedStatus());
        order.setTrelloCard(card.id());
        order.setTrelloUrl(card.url());
        orders.flush();
        return response(order);
    }

    private boolean validUrl(String value) {
        if (value == null || value.length() > 2048) return false;
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && "trello.com".equals(uri.getHost())
                    && uri.getUserInfo() == null && uri.getPort() == -1 && uri.getPath().startsWith("/c/");
        } catch (IllegalArgumentException exception) { return false; }
    }

    private WorkOrderTrelloResponse response(WorkOrder order) {
        return new WorkOrderTrelloResponse(order.getBlankNumber(), order.getTrelloCard(), order.getTrelloUrl(),
                order.getTrelloSyncStatus().getCode());
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
