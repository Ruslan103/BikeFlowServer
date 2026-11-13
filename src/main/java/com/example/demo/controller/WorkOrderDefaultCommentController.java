package com.example.demo.controller;

import com.example.demo.dto.workorder.WorkOrderDefaultCommentRequest;
import com.example.demo.dto.workorder.WorkOrderDefaultCommentResponse;
import com.example.demo.service.workorder.WorkOrderDefaultCommentService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-order-default-comment")
@RequiredArgsConstructor
@Slf4j
public class WorkOrderDefaultCommentController {
    private final WorkOrderDefaultCommentService comments;

    @GetMapping
    public WorkOrderDefaultCommentResponse read() {
        log.info("Получен API-запрос на получение комментария к заказу по умолчанию");
        return comments.find();
    }

    @PostMapping
    public ResponseEntity<WorkOrderDefaultCommentResponse> create(
            @Valid @RequestBody WorkOrderDefaultCommentRequest request) {
        log.info("Получен API-запрос на создание комментария к заказу по умолчанию");
        WorkOrderDefaultCommentResponse response = comments.create(request);
        return ResponseEntity.created(URI.create("/api/work-order-default-comment")).body(response);
    }

    @PutMapping
    public WorkOrderDefaultCommentResponse update(
            @Valid @RequestBody WorkOrderDefaultCommentRequest request) {
        log.info("Получен API-запрос на изменение комментария к заказу по умолчанию");
        return comments.update(request);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        log.info("Получен API-запрос на удаление комментария к заказу по умолчанию");
        comments.delete();
        return ResponseEntity.noContent().build();
    }
}
