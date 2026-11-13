package com.example.setup;

import com.example.demo.dto.auth.CsrfResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SetupAuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;

    @GetMapping("/api/auth/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        log.info("Подготовлен CSRF-токен режима настройки");
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/api/auth/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request, HttpServletResponse response) {
        log.info("Получен запрос на вход в кабинет первоначальной настройки");
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.login(), body.password()));
        request.getSession();
        request.changeSessionId();
        csrfTokens.saveToken(null, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        log.info("Вход в кабинет первоначальной настройки выполнен");
        return new UserResponse(null, authentication.getName(), "Administrator", Set.of("ADMIN"), true, null);
    }
}
