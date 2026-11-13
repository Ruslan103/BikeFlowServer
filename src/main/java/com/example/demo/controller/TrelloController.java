package com.example.demo.controller;

import com.example.demo.dto.trello.TrelloBoardResponse;
import com.example.demo.dto.trello.TrelloCardResponse;
import com.example.demo.dto.trello.TrelloLabelResponse;
import com.example.demo.dto.trello.TrelloListResponse;
import com.example.demo.dto.trello.TrelloMemberResponse;
import com.example.demo.service.trello.TrelloCatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trello")
@RequiredArgsConstructor
public class TrelloController {
    private final TrelloCatalogService catalog;
    @GetMapping("/connection") public TrelloMemberResponse connection() { return catalog.connection(); }
    @GetMapping("/boards") public List<TrelloBoardResponse> boards() { return catalog.boards(); }
    @GetMapping("/boards/{id}/lists") public List<TrelloListResponse> lists(@PathVariable String id) { return catalog.lists(id); }
    @GetMapping("/boards/{id}/labels") public List<TrelloLabelResponse> labels(@PathVariable String id) { return catalog.labels(id); }
    @GetMapping("/lists/{id}/cards") public List<TrelloCardResponse> cards(@PathVariable String id) { return catalog.cards(id); }
}
