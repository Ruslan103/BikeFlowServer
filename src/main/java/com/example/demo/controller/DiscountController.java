package com.example.demo.controller;

import com.example.demo.dto.discount.DiscountRequest;
import com.example.demo.dto.discount.DiscountResponse;
import com.example.demo.dto.discount.DiscountBatchResponse;
import com.example.demo.service.discount.DiscountService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {
    private final DiscountService discounts;

    @PostMapping
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountRequest request) {
        DiscountResponse discount = discounts.create(request);
        return ResponseEntity.created(URI.create("/api/discounts/" + discount.id())).body(discount);
    }

    @GetMapping
    public List<DiscountResponse> findAll() {
        return discounts.findAll();
    }

    @GetMapping("/batch")
    public DiscountBatchResponse findBatch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return discounts.findBatch(page, size);
    }

    @GetMapping("/search")
    public List<DiscountResponse> search(@RequestParam String query) {
        return discounts.search(query);
    }

    @GetMapping("/{id}")
    public DiscountResponse findById(@PathVariable Long id) {
        return discounts.findById(id);
    }

    @PutMapping("/{id}")
    public DiscountResponse update(@PathVariable Long id, @Valid @RequestBody DiscountRequest request) {
        return discounts.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        discounts.delete(id);
        return ResponseEntity.noContent().build();
    }
}
