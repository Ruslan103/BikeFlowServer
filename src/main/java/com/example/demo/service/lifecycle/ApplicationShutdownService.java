package com.example.demo.service.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationShutdownService {
    private final ConfigurableApplicationContext context;
    private final Environment environment;
    private final AtomicBoolean shutdownScheduled = new AtomicBoolean();

    public boolean shutdown() {
        if (isDockerManaged()) {
            log.warn("Для остановки контейнерного сервера используйте docker compose stop");
            return false;
        }
        if (!shutdownScheduled.compareAndSet(false, true)) {
            log.warn("Выключение сервера уже запланировано");
            return false;
        }

        log.info("Подготовлено штатное выключение сервера");
        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(1_500);
                log.info("Выполняется штатное выключение сервера");
                context.close();
                System.exit(0);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("Выключение сервера было прервано", exception);
            } catch (RuntimeException exception) {
                log.error("Ошибка при штатном выключении сервера", exception);
                System.exit(1);
            }
        }, "bike-server-shutdown");
        shutdownThread.setDaemon(false);
        shutdownThread.start();
        return true;
    }

    public boolean isDockerManaged() {
        return environment.getProperty("BIKE_DOCKER", Boolean.class, false);
    }
}
