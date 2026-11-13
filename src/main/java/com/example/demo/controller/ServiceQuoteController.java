package com.example.demo.controller;

import com.example.demo.dto.workorder.ServiceQuoteRequest;
import com.example.demo.dto.workorder.ServiceQuoteResponse;
import com.example.demo.service.workorder.ServiceQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ServiceQuoteController {
    private final ServiceQuoteService quotes;

    @PostMapping("/api/work-orders/services/quote")
    public ServiceQuoteResponse calculate(@Valid @RequestBody ServiceQuoteRequest request) {
        return quotes.calculate(request);
    }
}
