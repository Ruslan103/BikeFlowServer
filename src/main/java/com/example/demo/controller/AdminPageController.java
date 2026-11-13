package com.example.demo.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AdminPageController {
    @GetMapping("/")
    public String home() {
        log.info("Открыта главная страница кабинета");
        return "redirect:/admin";
    }

    @GetMapping("/login")
    @ResponseBody
    public ResponseEntity<Resource> login() {
        log.info("Открыта страница входа");
        return page("login.html");
    }

    @GetMapping("/admin")
    @ResponseBody
    public ResponseEntity<Resource> settings() {
        log.info("Открыта страница настроек");
        return page("settings.html");
    }

    @GetMapping("/admin/logs")
    @ResponseBody
    public ResponseEntity<Resource> logs() {
        log.info("Открыта страница журнала сервера");
        return page("logs.html");
    }

    @GetMapping("/admin/users")
    @ResponseBody
    public ResponseEntity<Resource> users() {
        log.info("Открыта страница управления учётными записями");
        return page("users.html");
    }

    private ResponseEntity<Resource> page(String name) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("admin-pages/" + name));
    }
}
