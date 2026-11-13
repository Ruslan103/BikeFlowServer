package com.example.demo.service.settings;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Serves only the public certificate over HTTP so clients can bootstrap HTTPS trust. */
@Component
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
@Slf4j
public class TlsCertificateBootstrapServer {
    private final TlsCertificateService certificates;
    private final int port;
    private HttpServer server;

    public TlsCertificateBootstrapServer(
            TlsCertificateService certificates,
            @Value("${bike.tls.bootstrap-http-port:8080}") int port) {
        this.certificates = certificates;
        this.port = port;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/tls/certificate", this::serveCertificate);
            server.start();
            log.info("Запущен HTTP endpoint первичной загрузки сертификата на порту {}", port);
        } catch (IOException exception) {
            log.warn("Не удалось запустить HTTP endpoint первичной загрузки сертификата на порту {}", port, exception);
        }
    }

    @org.springframework.context.event.EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void stop() {
        if (server != null) server.stop(1);
    }

    private void serveCertificate(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())
                || !"/api/tls/certificate".equals(exchange.getRequestURI().getPath())) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        try {
            TlsCertificateService.PublicCertificate certificate = certificates.downloadPublicCertificate();
            exchange.getResponseHeaders().set("Content-Type", "application/x-pem-file");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"server-tls.crt\"");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Certificate-SHA256", certificate.sha256Fingerprint());
            exchange.sendResponseHeaders(200, certificate.content().length);
            try (var body = exchange.getResponseBody()) {
                body.write(certificate.content());
            }
        } catch (RuntimeException exception) {
            byte[] message = "Сертификат сервера не выпущен.".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, message.length);
            try (var body = exchange.getResponseBody()) {
                body.write(message);
            }
        }
    }
}
