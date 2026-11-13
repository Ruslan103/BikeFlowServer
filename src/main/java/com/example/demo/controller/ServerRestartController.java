package com.example.demo.controller;

import com.example.demo.service.lifecycle.ApplicationRestartService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/restart")
@RequiredArgsConstructor
@Slf4j
public class ServerRestartController {
    private final ApplicationRestartService restartService;

    @PostMapping
    public ResponseEntity<Map<String, String>> restart() {
        log.info("Получен запрос на перезапуск сервера");
        if (!restartService.restart()) {
            log.warn("Перезапуск сервера уже выполняется");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Перезапуск сервера уже выполняется."));
        }
        return ResponseEntity.accepted().body(Map.of("message", "Перезапуск сервера запущен."));
    }
}
