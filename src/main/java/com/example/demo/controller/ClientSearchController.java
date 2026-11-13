package com.example.demo.controller;

import com.example.demo.dto.client.ClientSearchResponse;
import com.example.demo.service.client.ClientSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientSearchController {
    private final ClientSearchService clients;

    @GetMapping("/search")
    public List<ClientSearchResponse> findByPhone(@RequestParam(name = "phone", required = false) String phone) {
        return clients.findByPhone(phone);
    }
}
