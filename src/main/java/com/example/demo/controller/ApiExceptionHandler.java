package com.example.demo.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ProblemDetail staleCard(org.springframework.orm.ObjectOptimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Карточка изменена другим пользователем. Перечитайте данные.");
    }
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail authentication(AuthenticationException exception) {
        log.warn("Запрос отклонён: ошибка аутентификации");
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid login or password");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail status(ResponseStatusException exception) {
        log.warn("Запрос завершился с HTTP-статусом {}", exception.getStatusCode().value());
        var problem = ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason() == null
                ? "Request failed" : exception.getReason());
        if (exception instanceof com.example.demo.service.client.ClientCardConflict conflict) {
            problem.setProperty("code", conflict.code());
            if (conflict.searchQuery() != null) problem.setProperty("searchQuery", conflict.searchQuery());
        }
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException exception) {
        log.warn("Запрос отклонён: данные не прошли проверку");
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict(DataIntegrityViolationException exception) {
        log.warn("Запрос отклонён: конфликт данных");
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data conflicts with an existing record");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception exception) {
        log.error("При обработке запроса произошла внутренняя ошибка", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера");
    }
}
