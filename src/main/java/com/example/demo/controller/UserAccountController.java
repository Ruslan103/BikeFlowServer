package com.example.demo.controller;

import com.example.demo.dto.auth.*;
import com.example.demo.service.auth.UserAccountService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserAccountController {
    private final UserAccountService users;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        log.info("Получен запрос на создание учётной записи");
        UserResponse user = users.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }

    @GetMapping
    public UserPageResponse findAll(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) Boolean enabled,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        log.info("Получен запрос на список учётных записей");
        var result = users.findAll(search, enabled, page, size);
        return new UserPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        log.info("Получен запрос на просмотр текущей учётной записи");
        return users.findById(currentId(authentication));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Получен запрос на смену собственного пароля");
        users.changePassword(currentId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        log.info("Получен запрос на завершение всех собственных сессий");
        users.logoutAll(currentId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public UserResponse findById(@PathVariable Long userId) {
        log.info("Получен запрос на просмотр учётной записи");
        return users.findById(userId);
    }

    @PutMapping("/{userId}")
    public UserResponse update(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        log.info("Получен запрос на изменение учётной записи");
        return users.update(userId, request);
    }

    @PutMapping("/{userId}/roles")
    public UserResponse roles(@PathVariable Long userId, @Valid @RequestBody UpdateRolesRequest request) {
        log.info("Получен запрос на изменение ролей учётной записи");
        return users.updateRoles(userId, request);
    }

    @PutMapping("/{userId}/enabled")
    public UserResponse enabled(@PathVariable Long userId, @Valid @RequestBody UpdateEnabledRequest request) {
        log.info("Получен запрос на изменение активности учётной записи");
        return users.updateEnabled(userId, request.enabled());
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long userId,
                                              @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Получен запрос на сброс пароля учётной записи");
        users.resetPassword(userId, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/logout-all")
    public ResponseEntity<Void> logoutAll(@PathVariable Long userId) {
        log.info("Получен запрос на завершение всех сессий учётной записи");
        users.logoutAll(userId);
        return ResponseEntity.noContent().build();
    }

    private Long currentId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
