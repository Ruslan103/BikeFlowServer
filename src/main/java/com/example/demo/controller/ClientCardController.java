package com.example.demo.controller;

import com.example.demo.dto.client.BicycleCardRequest;
import com.example.demo.dto.client.ClientBatchResponse;
import com.example.demo.dto.client.ClientCardRequest;
import com.example.demo.dto.client.ClientSearchResponse;
import com.example.demo.dto.client.ClientSearchResult;
import com.example.demo.service.client.ClientCardService;
import com.example.demo.service.client.ClientSearchService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientCardController {
    private final ClientCardService cards;
    private final ClientSearchService search;
    @GetMapping public ClientSearchResult search(@RequestParam(defaultValue = "") String query) { return search.search(query); }
    @GetMapping("/batch")
    public ClientBatchResponse findBatch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return search.findBatch(page, size);
    }
    @GetMapping("/{id}") public ClientSearchResponse find(@PathVariable Long id) { return search.findById(id); }
    @PostMapping public ResponseEntity<ClientSearchResponse> create(@Valid @RequestBody ClientCardRequest request) {
        var client = cards.create(request);
        return ResponseEntity.created(URI.create("/api/clients/" + client.id())).body(client);
    }
    @PutMapping("/{id}") public ClientSearchResponse update(@PathVariable Long id, @Valid @RequestBody ClientCardRequest request) {
        return cards.update(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cards.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/bicycles") public ClientSearchResponse createBicycle(@PathVariable Long id, @Valid @RequestBody BicycleCardRequest request) {
        return cards.createBicycle(id, request);
    }
    @PutMapping("/{id}/bicycles/{bicycleId}") public ClientSearchResponse updateBicycle(@PathVariable Long id,
            @PathVariable Long bicycleId, @Valid @RequestBody BicycleCardRequest request) {
        return cards.updateBicycle(id, bicycleId, request);
    }
    @DeleteMapping("/{id}/bicycles/{bicycleId}")
    public ClientSearchResponse deleteBicycle(@PathVariable Long id, @PathVariable Long bicycleId, @RequestParam Long version) {
        return cards.deleteBicycle(id, bicycleId, version);
    }
}
