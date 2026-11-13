package com.example.demo.service.settings;

import com.example.demo.dto.settings.ServerAddressesResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerAddressService {
    private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(2);
    private static final Pattern IPV4_PATTERN = Pattern.compile("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(LOOKUP_TIMEOUT)
            .build();

    private final TlsCertificateService certificates;
    private final Environment environment;

    public ServerAddressesResponse detectAddresses() {
        int port = environment.getProperty("server.port", Integer.class, 8443);
        List<String> localBaseUrls = new ArrayList<>();
        localBaseUrls.add(baseUrl("localhost", port));
        certificates.detectLocalIpv4Addresses().stream()
                .map(address -> baseUrl(address, port))
                .forEach(localBaseUrls::add);
        String publicBaseUrl = detectPublicIpv4()
                .map(address -> baseUrl(address, port))
                .orElse(null);
        return new ServerAddressesResponse(localBaseUrls, publicBaseUrl);
    }

    private Optional<String> detectPublicIpv4() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.ipify.org"))
                .timeout(LOOKUP_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            String address = response.body().trim();
            if (response.statusCode() == 200 && isIpv4(address)) {
                return Optional.of(address);
            }
            log.debug("Сервис определения публичного IP вернул некорректный ответ");
        } catch (IOException exception) {
            log.debug("Не удалось определить публичный IP-адрес сервера");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.debug("Определение публичного IP-адреса прервано");
        }
        return Optional.empty();
    }

    private boolean isIpv4(String value) {
        if (!IPV4_PATTERN.matcher(value).matches()) return false;
        for (String octet : value.split("\\.")) {
            if (Integer.parseInt(octet) > 255) return false;
        }
        return true;
    }

    private String baseUrl(String host, int port) {
        return "https://" + host + ":" + port;
    }
}
