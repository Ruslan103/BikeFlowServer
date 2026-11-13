package com.example.demo.service.lifecycle;

import com.example.demo.DemoApplication;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationRestartService {
    private final ConfigurableApplicationContext context;
    private final ApplicationArguments arguments;
    private final Environment environment;
    private final AtomicBoolean restartScheduled = new AtomicBoolean();

    public boolean restart() {
        if (!restartScheduled.compareAndSet(false, true)) {
            log.warn("Перезапуск сервера уже запланирован");
            return false;
        }
        log.info("Подготовлен перезапуск сервера");
        String[] normalArguments = Arrays.stream(arguments.getSourceArgs())
                .filter(argument -> !"--setup".equals(argument))
                .toArray(String[]::new);
        boolean startedInSetupMode = environment.getProperty("bike.setup-mode", Boolean.class, false);
        boolean dockerManaged = environment.getProperty("BIKE_DOCKER", Boolean.class, false);
        Optional<ProcessBuilder> processRestart = dockerManaged
                ? Optional.empty() : processRestartCommand(normalArguments);

        Thread restartThread = new Thread(() -> {
            try {
                Thread.sleep(1_500);
                if (dockerManaged) {
                    log.info("Завершение приложения для перезапуска Docker-контейнера");
                    context.close();
                    System.exit(0);
                    return;
                }
                if (processRestart.isPresent()) {
                    log.info("Перезапускается JVM для приложения, запущенного из JAR");
                    processRestart.get().start();
                    context.close();
                    System.exit(0);
                    return;
                }
                log.info("Перезапускается контекст Spring приложения");
                context.close();
                try {
                    DemoApplication.createApplication(false).run(normalArguments);
                } catch (RuntimeException exception) {
                    if (!startedInSetupMode) {
                        throw exception;
                    }
                    log.error("Обычный режим не запустился; выполняется возврат в режим настройки", exception);
                    String[] setupArguments = Stream.concat(Arrays.stream(normalArguments),
                            Stream.of("--setup")).toArray(String[]::new);
                    DemoApplication.createApplication(true).run(setupArguments);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("Перезапуск сервера был прерван", exception);
            } catch (IOException | RuntimeException exception) {
                log.error("Не удалось запустить сервер после перезапуска", exception);
            }
        }, "bike-server-restart");
        restartThread.setDaemon(false);
        restartThread.start();
        return true;
    }

    private Optional<ProcessBuilder> processRestartCommand(String[] applicationArguments) {
        log.debug("Проверяется возможность перезапуска JVM-процесса");
        ProcessHandle.Info processInfo = ProcessHandle.current().info();
        Optional<String> javaCommand = processInfo.command();
        Optional<String[]> currentArguments = processInfo.arguments();
        if (javaCommand.isEmpty() || currentArguments.isEmpty()) {
            log.debug("Параметры текущего JVM-процесса недоступны, будет перезапущен Spring-контекст");
            return Optional.empty();
        }

        List<String> arguments = Arrays.asList(currentArguments.get());
        int jarOption = arguments.indexOf("-jar");
        if (jarOption < 0 || jarOption + 1 >= arguments.size()) {
            log.debug("Приложение запущено не через JAR, будет перезапущен Spring-контекст");
            return Optional.empty();
        }

        List<String> restartedArguments = new ArrayList<>(arguments.subList(0, jarOption + 2));
        Arrays.stream(applicationArguments)
                .filter(argument -> !argument.startsWith("--bike.wait-for-pid="))
                .forEach(restartedArguments::add);
        restartedArguments.add("--bike.wait-for-pid=" + ProcessHandle.current().pid());

        ProcessBuilder processBuilder = new ProcessBuilder(javaCommand.get());
        processBuilder.command().addAll(restartedArguments);
        processBuilder.directory(Path.of(System.getProperty("user.dir")).toFile());
        processBuilder.environment().remove("BIKE_SETUP_MODE");
        processBuilder.inheritIO();
        log.debug("Подготовлены параметры запуска нового JVM-процесса");
        return Optional.of(processBuilder);
    }
}
