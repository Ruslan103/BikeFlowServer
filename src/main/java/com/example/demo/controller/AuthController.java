package com.example.demo.controller;

import com.example.demo.dto.auth.CsrfResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.UserResponse;
import com.example.demo.service.auth.UserAccountService;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserAccountRepository;
import com.example.demo.security.AccountSessionFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final UserAccountService userService;

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        log.info("Подготовлен CSRF-токен для запроса");
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/login")
    @Transactional
    public UserResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request, HttpServletResponse response) {
        log.info("Получен запрос на вход в систему");
        roles.lockAdminRole().orElseThrow();
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.login(), body.password()));
        Long id = Long.valueOf(authentication.getName());
        var account = users.findById(id).orElseThrow();
        request.getSession();
        request.changeSessionId();
        csrfTokens.saveToken(null, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession().setAttribute(AccountSessionFilter.VERSION_ATTRIBUTE, account.getSecurityVersion());
        contexts.saveContext(context, request, response);
        return userService.findById(id);
    }
}
