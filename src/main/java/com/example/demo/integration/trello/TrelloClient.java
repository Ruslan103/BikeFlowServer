package com.example.demo.integration.trello;

import com.example.demo.dto.trello.TrelloBoardResponse;
import com.example.demo.dto.trello.TrelloCardResponse;
import com.example.demo.dto.trello.TrelloLabelResponse;
import com.example.demo.dto.trello.TrelloListResponse;
import com.example.demo.dto.trello.TrelloMemberResponse;
import java.util.List;

public interface TrelloClient {
    TrelloMemberResponse member();
    List<TrelloBoardResponse> boards();
    List<TrelloListResponse> lists(String boardId);
    List<TrelloLabelResponse> labels(String boardId);
    List<TrelloCardResponse> cards(String listId);
    TrelloCardResponse card(String cardId);
    TrelloListResponse list(String listId);
    List<TrelloCardResponse> boardCards(String boardId);
    TrelloCardResponse createCard(String listId, String name, String description, List<String> labelIds);
    TrelloCardResponse updateCard(String cardId, String boardId, String listId, String name, String description, List<String> labelIds);
}
