package com.example.demo.controller;

import com.example.demo.dto.WorkOrderNumberResponse;
import com.example.demo.dto.WorkOrderRequest;
import com.example.demo.dto.WorkOrderResponse;
import com.example.demo.dto.product.WorkOrderCustomerPartRequest;
import com.example.demo.dto.product.WorkOrderCustomerPartResponse;
import com.example.demo.dto.product.WorkOrderProductItemRequest;
import com.example.demo.dto.product.WorkOrderProductItemResponse;
import com.example.demo.service.workorder.WorkOrderService;
import com.example.demo.service.workorder.WorkOrderNumberService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Slf4j
public class WorkOrderController {

    private final WorkOrderNumberService workOrderNumberService;
    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderRequest request) {
        log.info("Получен запрос на создание бланка");
        WorkOrderResponse response = workOrderService.create(request);
        URI location = URI.create("/api/work-orders/" + response.blankNumber());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<WorkOrderResponse> findAll() {
        log.info("Получен запрос на список бланков");
        return workOrderService.findAll();
    }

    @GetMapping("/{blankNumber}")
    public List<WorkOrderResponse> findByNumber(@PathVariable String blankNumber) {
        log.info("Получен запрос на поиск бланка по номеру");
        return workOrderService.findByNumber(blankNumber);
    }

    @PutMapping("/{blankNumber}")
    public WorkOrderResponse update(
            @PathVariable String blankNumber,
            @Valid @RequestBody WorkOrderRequest request
    ) {
        log.info("Получен запрос на изменение бланка");
        return workOrderService.update(blankNumber, request);
    }

    @DeleteMapping("/{blankNumber}")
    public ResponseEntity<Void> delete(@PathVariable String blankNumber) {
        log.info("Получен запрос на удаление бланка");
        workOrderService.delete(blankNumber);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{blankNumber}/products")
    public List<WorkOrderProductItemResponse> findProducts(@PathVariable String blankNumber) {
        return workOrderService.findProducts(blankNumber);
    }

    @PostMapping("/{blankNumber}/products")
    public ResponseEntity<WorkOrderProductItemResponse> addProduct(
            @PathVariable String blankNumber, @Valid @RequestBody WorkOrderProductItemRequest request) {
        WorkOrderProductItemResponse item = workOrderService.addProduct(blankNumber, request);
        return ResponseEntity.created(URI.create("/api/work-orders/" + blankNumber + "/products/" + item.id()))
                .body(item);
    }

    @GetMapping("/{blankNumber}/products/{itemId}")
    public WorkOrderProductItemResponse findProduct(@PathVariable String blankNumber, @PathVariable Long itemId) {
        return workOrderService.findProduct(blankNumber, itemId);
    }

    @PutMapping("/{blankNumber}/products/{itemId}")
    public WorkOrderProductItemResponse updateProduct(@PathVariable String blankNumber, @PathVariable Long itemId,
            @Valid @RequestBody WorkOrderProductItemRequest request) {
        return workOrderService.updateProduct(blankNumber, itemId, request);
    }

    @DeleteMapping("/{blankNumber}/products/{itemId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String blankNumber, @PathVariable Long itemId) {
        workOrderService.deleteProduct(blankNumber, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{blankNumber}/customer-parts")
    public List<WorkOrderCustomerPartResponse> findCustomerParts(@PathVariable String blankNumber) {
        return workOrderService.findCustomerParts(blankNumber);
    }

    @PostMapping("/{blankNumber}/customer-parts")
    public ResponseEntity<WorkOrderCustomerPartResponse> addCustomerPart(
            @PathVariable String blankNumber, @Valid @RequestBody WorkOrderCustomerPartRequest request) {
        WorkOrderCustomerPartResponse part = workOrderService.addCustomerPart(blankNumber, request);
        return ResponseEntity.created(URI.create("/api/work-orders/" + blankNumber + "/customer-parts/" + part.id()))
                .body(part);
    }

    @GetMapping("/{blankNumber}/customer-parts/{partId}")
    public WorkOrderCustomerPartResponse findCustomerPart(@PathVariable String blankNumber, @PathVariable Long partId) {
        return workOrderService.findCustomerPart(blankNumber, partId);
    }

    @PutMapping("/{blankNumber}/customer-parts/{partId}")
    public WorkOrderCustomerPartResponse updateCustomerPart(@PathVariable String blankNumber, @PathVariable Long partId,
            @Valid @RequestBody WorkOrderCustomerPartRequest request) {
        return workOrderService.updateCustomerPart(blankNumber, partId, request);
    }

    @DeleteMapping("/{blankNumber}/customer-parts/{partId}")
    public ResponseEntity<Void> deleteCustomerPart(@PathVariable String blankNumber, @PathVariable Long partId) {
        workOrderService.deleteCustomerPart(blankNumber, partId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/number")
    public WorkOrderNumberResponse generateNumber() {
        log.info("Получен запрос на генерацию номера бланка");
        return new WorkOrderNumberResponse(workOrderNumberService.generate());
    }
}
