package com.example.demo.controller;

import com.example.demo.dto.product.CreateProductPriceRequest;
import com.example.demo.dto.product.ProductPriceResponse;
import com.example.demo.dto.product.UpdateProductPriceRequest;
import com.example.demo.service.catalog.ProductPriceService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/prices")
@RequiredArgsConstructor
public class ProductPriceController {
    private final ProductPriceService prices;

    @PostMapping
    public ResponseEntity<ProductPriceResponse> create(@PathVariable Long productId,
                                                        @Valid @RequestBody CreateProductPriceRequest request) {
        ProductPriceResponse price = prices.create(productId, request);
        return ResponseEntity.created(URI.create("/api/products/" + productId + "/prices/" + price.id())).body(price);
    }

    @GetMapping
    public List<ProductPriceResponse> findAll(@PathVariable Long productId) {
        return prices.findAll(productId);
    }

    @GetMapping("/{priceId}")
    public ProductPriceResponse findById(@PathVariable Long productId, @PathVariable Long priceId) {
        return prices.findById(productId, priceId);
    }

    @PutMapping("/{priceId}")
    public ProductPriceResponse update(@PathVariable Long productId, @PathVariable Long priceId,
                                       @Valid @RequestBody UpdateProductPriceRequest request) {
        return prices.update(productId, priceId, request);
    }

    @DeleteMapping("/{priceId}")
    public ResponseEntity<Void> delete(@PathVariable Long productId, @PathVariable Long priceId) {
        prices.delete(productId, priceId);
        return ResponseEntity.noContent().build();
    }
}
