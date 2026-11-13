package com.example.demo.service.trello;

import com.example.demo.dto.trello.CreateWorkOrderCardRequest;
import com.example.demo.dto.trello.LinkTrelloCardRequest;
import com.example.demo.dto.trello.WorkOrderTrelloResponse;

public interface WorkOrderTrelloService {
    com.example.demo.dto.trello.TrelloCardResponse readCard(String number);
    WorkOrderTrelloResponse link(String number, LinkTrelloCardRequest request);
    WorkOrderTrelloResponse createCard(String number, CreateWorkOrderCardRequest request);
}
