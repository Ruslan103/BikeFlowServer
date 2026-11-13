package com.example.demo.controller;

import com.example.demo.dto.trello.CreateWorkOrderCardRequest;
import com.example.demo.dto.trello.LinkTrelloCardRequest;
import com.example.demo.dto.trello.WorkOrderTrelloResponse;
import com.example.demo.service.trello.WorkOrderTrelloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/work-orders/{number}/trello")
@RequiredArgsConstructor
public class WorkOrderTrelloController {
    private final WorkOrderTrelloService service;
    @GetMapping public com.example.demo.dto.trello.TrelloCardResponse read(@PathVariable String number) {
        return service.readCard(number);
    }
    @PutMapping public WorkOrderTrelloResponse link(@PathVariable String number, @Valid @RequestBody LinkTrelloCardRequest request) {
        return service.link(number, request);
    }
    @PostMapping("/card") public WorkOrderTrelloResponse create(@PathVariable String number, @Valid @RequestBody CreateWorkOrderCardRequest request) {
        return service.createCard(number, request);
    }
}
