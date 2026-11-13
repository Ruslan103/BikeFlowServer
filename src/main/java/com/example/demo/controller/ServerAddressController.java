package com.example.demo.controller;

import com.example.demo.dto.settings.ServerAddressesResponse;
import com.example.demo.service.settings.ServerAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/server-addresses")
@RequiredArgsConstructor
public class ServerAddressController {
    private final ServerAddressService addresses;

    @GetMapping
    public ResponseEntity<ServerAddressesResponse> read() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(addresses.detectAddresses());
    }
}
