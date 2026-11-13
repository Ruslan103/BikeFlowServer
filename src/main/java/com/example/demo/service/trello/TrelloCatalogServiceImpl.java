package com.example.demo.service.trello;

import com.example.demo.dto.trello.TrelloBoardResponse;
import com.example.demo.dto.trello.TrelloCardResponse;
import com.example.demo.dto.trello.TrelloLabelResponse;
import com.example.demo.dto.trello.TrelloListResponse;
import com.example.demo.dto.trello.TrelloMemberResponse;
import com.example.demo.integration.trello.TrelloClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TrelloCatalogServiceImpl implements TrelloCatalogService {
    private final TrelloClient client;
    @Override public TrelloMemberResponse connection() { return client.member(); }
    @Override public List<TrelloBoardResponse> boards() { return client.boards(); }
    @Override public List<TrelloListResponse> lists(String boardId) { validateId(boardId); return client.lists(boardId); }
    @Override public List<TrelloLabelResponse> labels(String boardId) { validateId(boardId); return client.labels(boardId); }
    @Override public List<TrelloCardResponse> cards(String listId) { validateId(listId); return client.cards(listId); }

    private void validateId(String id) {
        if (id == null || !id.matches("[a-fA-F0-9]{24}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный ID объекта Trello.");
        }
    }
}
