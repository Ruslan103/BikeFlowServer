package com.example.demo.controller;

import com.example.demo.dto.service.CreateWorkshopServiceRequest;
import com.example.demo.dto.service.WorkshopServiceResponse;
import com.example.demo.dto.service.WorkshopServiceBatchResponse;
import com.example.demo.dto.service.UpdateWorkshopServiceRequest;
import com.example.demo.service.catalog.WorkshopCatalogService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Slf4j
public class WorkshopServiceController {
    private final WorkshopCatalogService catalog;

    @PostMapping
    public ResponseEntity<WorkshopServiceResponse> create(@Valid @RequestBody CreateWorkshopServiceRequest request) {
        log.info("Получен запрос на создание услуги");
        WorkshopServiceResponse service = catalog.create(request);
        return ResponseEntity.created(URI.create("/api/services/" + service.id())).body(service);
    }

    @GetMapping
    public List<WorkshopServiceResponse> findAll() {
        return catalog.findAll();
    }

    @GetMapping("/batch")
    public WorkshopServiceBatchResponse findBatch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        log.info("Получен запрос на пачку услуг: страница {}, размер {}", page, size);
        return catalog.findBatch(page, size);
    }

    @GetMapping("/search")
    public List<WorkshopServiceResponse> searchByName(@RequestParam String query) {
        log.info("Получен запрос на поиск услуги по названию");
        return catalog.searchByName(query);
    }

    @GetMapping("/{id}")
    public WorkshopServiceResponse findById(@PathVariable Long id) {
        return catalog.findById(id);
    }

    @PutMapping("/{id}")
    public WorkshopServiceResponse update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateWorkshopServiceRequest request) {
        log.info("Получен запрос на обновление услуги, ID {}", id);
        return catalog.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Получен запрос на удаление услуги, ID {}", id);
        catalog.delete(id);
        return ResponseEntity.noContent().build();
    }
}
