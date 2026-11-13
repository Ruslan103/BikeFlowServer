package com.example.demo.config;

import com.example.demo.dto.auth.CreateUserRequest;
import com.example.demo.service.auth.UserAccountService;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {
    private final Environment environment;
    private final UserAccountService users;
    private final Validator validator;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Проверка начальной настройки администратора");
        String login = environment.getProperty("BIKE_ADMIN_LOGIN");
        String password = environment.getProperty("BIKE_ADMIN_PASSWORD");
        if (login == null && password == null) {
            log.info("Данные для начального создания администратора не заданы");
            return;
        }
        var request = new CreateUserRequest(login, "Administrator", password, Set.of("ADMIN"), true);
        if (!validator.validate(request).isEmpty()) {
            log.error("Не удалось проверить данные начального администратора");
            throw new IllegalStateException("Invalid BIKE_ADMIN_LOGIN or BIKE_ADMIN_PASSWORD configuration");
        }
        users.bootstrap(login, password);
        log.info("Начальная проверка администратора завершена");
    }
}
