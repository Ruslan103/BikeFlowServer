package com.example.demo.controller;

import com.example.demo.dto.service.CreateServicePriceRequest;
import com.example.demo.dto.service.ServicePriceResponse;
import com.example.demo.dto.service.UpdateServicePriceRequest;
import com.example.demo.service.catalog.ServicePriceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services/{serviceId}/prices")
@RequiredArgsConstructor
public class ServicePriceController {
    private final ServicePriceService prices;

    @PostMapping
    public ResponseEntity<ServicePriceResponse> create(@PathVariable Long serviceId,
                                                       @Valid @RequestBody CreateServicePriceRequest request) {
        ServicePriceResponse price = prices.create(serviceId, request);
        return ResponseEntity.created(URI.create("/api/services/" + serviceId + "/prices/" + price.id())).body(price);
    }

    @GetMapping
    public List<ServicePriceResponse> findAll(@PathVariable Long serviceId) {
        return prices.findAll(serviceId);
    }

    @GetMapping("/{priceId}")
    public ServicePriceResponse findById(@PathVariable Long serviceId, @PathVariable Long priceId) {
        return prices.findById(serviceId, priceId);
    }

    @PutMapping("/{priceId}")
    public ServicePriceResponse update(@PathVariable Long serviceId, @PathVariable Long priceId,
                                       @Valid @RequestBody UpdateServicePriceRequest request) {
        return prices.update(serviceId, priceId, request);
    }

    @DeleteMapping("/{priceId}")
    public ResponseEntity<Void> delete(@PathVariable Long serviceId, @PathVariable Long priceId) {
        prices.delete(serviceId, priceId);
        return ResponseEntity.noContent().build();
    }
}
