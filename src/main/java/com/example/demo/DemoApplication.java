package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.example.setup.SetupApplication;
import com.example.demo.service.settings.TlsCertificateService;
import java.util.Arrays;
import java.util.Map;
import java.util.OptionalLong;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class DemoApplication {

	public static void main(String[] args) {
		log.info("Подготовка к запуску сервера");
		OptionalLong parentPid = restartParentPid(args);
		if (parentPid.isPresent()) {
			log.info("Ожидание завершения предыдущего JVM-процесса");
			waitForParentExit(parentPid.getAsLong());
		}
		String[] applicationArgs = Arrays.stream(args)
				.filter(argument -> !argument.startsWith("--bike.wait-for-pid="))
				.toArray(String[]::new);
		boolean setup = Arrays.asList(applicationArgs).contains("--setup")
				|| Boolean.parseBoolean(System.getenv("BIKE_SETUP_MODE"));
		log.info("Запускается сервер в режиме: {}", setup ? "первоначальная настройка" : "обычная работа");
		try {
			createApplication(setup).run(applicationArgs);
		} catch (RuntimeException exception) {
			if (parentPid.isEmpty()) {
				log.error("Не удалось запустить сервер", exception);
				throw exception;
			}
			log.error("Обычный запуск после перезапуска не удался; возвращаемся к настройке", exception);
			String[] setupArguments = Arrays.copyOf(applicationArgs, applicationArgs.length + 1);
			setupArguments[applicationArgs.length] = "--setup";
			createApplication(true).run(setupArguments);
		}
	}

	private static OptionalLong restartParentPid(String[] args) {
		log.debug("Проверяются аргументы для сценария перезапуска");
		for (String argument : args) {
			if (argument.startsWith("--bike.wait-for-pid=")) {
				try {
					return OptionalLong.of(Long.parseLong(argument.substring("--bike.wait-for-pid=".length())));
				} catch (NumberFormatException exception) {
					throw new IllegalArgumentException("Invalid restart parent process id", exception);
				}
			}
		}
		return OptionalLong.empty();
	}

	private static void waitForParentExit(long parentPid) {
		log.info("Ожидание завершения предыдущего процесса сервера");
		long deadline = System.nanoTime() + java.time.Duration.ofSeconds(60).toNanos();
		while (ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false)) {
			if (System.nanoTime() >= deadline) {
				log.error("Предыдущий процесс сервера не завершился за отведённое время");
				throw new IllegalStateException("Timed out waiting for the previous server process to stop");
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for the previous server process", exception);
			}
		}
		log.info("Предыдущий процесс сервера завершён");
	}

	public static SpringApplication createApplication(boolean setup) {
		log.debug("Создаётся Spring-приложение для запуска в выбранном режиме");
		SpringApplication application = new SpringApplication(setup ? SetupApplication.class : DemoApplication.class);
		if (setup) {
			application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event ->
					event.getEnvironment().getPropertySources().addFirst(new MapPropertySource("setupMode", Map.of(
							"server.address", "127.0.0.1",
							"server.servlet.session.cookie.name", "BIKE_SETUP_SESSION",
							"bike.setup-mode", "true"))));
		} else {
			application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
				var environment = event.getEnvironment();
				if ("false".equalsIgnoreCase(environment.getProperty("server.ssl.enabled"))
						|| environment.containsProperty("server.ssl.key-store")) {
					return;
				}
				Map<String, Object> tlsProperties = new TlsCertificateService().ensureStartupCertificate();
				if (!tlsProperties.isEmpty()) {
					environment.getPropertySources().addFirst(new MapPropertySource("bikeStartupTls", tlsProperties));
				}
			});
		}
		return application;
	}

}
