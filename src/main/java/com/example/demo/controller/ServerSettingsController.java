package com.example.demo.controller;

import com.example.demo.dto.settings.ServerSettingsRequest;
import com.example.demo.dto.settings.ServerSettingsResponse;
import com.example.demo.service.settings.ServerSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.core.env.Environment;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@Slf4j
public class ServerSettingsController {
    private final ServerSettingsService settings;
    private final Environment environment;

    @GetMapping
    public ResponseEntity<ServerSettingsResponse> read() {
        log.info("Получен запрос на чтение настроек сервера");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(settings.read());
    }

    @PutMapping
    public ResponseEntity<ServerSettingsResponse> save(@Valid @RequestBody ServerSettingsRequest request,
                                                        Authentication authentication) {
        log.info("Получен запрос на сохранение настроек сервера");
        Long adminUserId = environment.getProperty("bike.setup-mode", Boolean.class, false)
                ? null : Long.parseLong(authentication.getName());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(settings.save(request, adminUserId));
    }
}
