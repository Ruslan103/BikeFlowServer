package com.example.demo.controller;

import com.example.demo.service.lifecycle.ApplicationShutdownService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shutdown")
@RequiredArgsConstructor
@Slf4j
public class ServerShutdownController {
    private final ApplicationShutdownService shutdownService;

    @PostMapping
    public ResponseEntity<Map<String, String>> shutdown() {
        log.info("Получен запрос на выключение сервера");
        if (!shutdownService.shutdown()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", shutdownService.isDockerManaged()
                            ? "Для остановки Docker-сервера выполните команду docker compose stop."
                            : "Выключение сервера уже выполняется."));
        }
        return ResponseEntity.accepted().body(Map.of("message", "Выключение сервера запущено."));
    }
}
