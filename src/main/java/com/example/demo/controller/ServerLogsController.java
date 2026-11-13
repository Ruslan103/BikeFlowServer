package com.example.demo.controller;

import com.example.demo.dto.settings.ServerLogsResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Slf4j
public class ServerLogsController {
    private static final int MAX_LINES = 500;
    private final Environment environment;

    @GetMapping
    public ResponseEntity<ServerLogsResponse> read() {
        Path logFile = Path.of(environment.getProperty("logging.file.name", "./logs/bike-server.log"))
                .toAbsolutePath().normalize();
        if (!Files.isRegularFile(logFile)) {
            log.debug("Файл журнала сервера пока не создан");
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new ServerLogsResponse(false, List.of()));
        }

        ArrayDeque<String> tail = new ArrayDeque<>(MAX_LINES);
        try (var lines = Files.lines(logFile)) {
            lines.forEach(line -> {
                if (tail.size() == MAX_LINES) {
                    tail.removeFirst();
                }
                tail.addLast(line);
            });
        } catch (IOException exception) {
            log.error("Не удалось прочитать файл журнала сервера", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось прочитать файл логов.", exception);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ServerLogsResponse(true, List.copyOf(tail)));
    }
}
