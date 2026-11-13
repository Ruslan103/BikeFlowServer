package com.example.demo.controller;

import com.example.demo.dto.product.ProductQuoteRequest;
import com.example.demo.dto.product.ProductQuoteResponse;
import com.example.demo.service.catalog.ProductQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductQuoteController {
    private final ProductQuoteService quotes;

    @PostMapping("/api/work-orders/products/quote")
    public ProductQuoteResponse calculate(@Valid @RequestBody ProductQuoteRequest request) {
        return quotes.calculate(request);
    }
}
