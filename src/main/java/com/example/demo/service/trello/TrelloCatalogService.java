package com.example.demo.service.trello;

import com.example.demo.dto.trello.TrelloBoardResponse;
import com.example.demo.dto.trello.TrelloCardResponse;
import com.example.demo.dto.trello.TrelloLabelResponse;
import com.example.demo.dto.trello.TrelloListResponse;
import com.example.demo.dto.trello.TrelloMemberResponse;
import java.util.List;

public interface TrelloCatalogService {
    TrelloMemberResponse connection();
    List<TrelloBoardResponse> boards();
    List<TrelloListResponse> lists(String boardId);
    List<TrelloLabelResponse> labels(String boardId);
    List<TrelloCardResponse> cards(String listId);
}
