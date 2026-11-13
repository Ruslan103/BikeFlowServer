package com.example.demo.service.settings;

import com.example.demo.dto.settings.TlsCertificateIssueRequest;
import com.example.demo.dto.settings.TlsCertificateIssueResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TlsCertificateService {
    private static final int MAX_KEYSTORE_BYTES = 5 * 1024 * 1024;
    private static final char[] PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@$%_-".toCharArray();

    private final Path configDirectory = Path.of("./config").toAbsolutePath().normalize();
    private final Path keyStorePath = configDirectory.resolve("server-tls.p12");
    private final Path settingsPath = configDirectory.resolve("server-tls.properties");

    /** Creates a first-run certificate before the embedded web server binds its HTTPS port. */
    public synchronized Map<String, Object> ensureStartupCertificate() {
        boolean keyStoreExists = Files.exists(keyStorePath);
        boolean settingsExist = Files.exists(settingsPath);
        if (keyStoreExists && settingsExist) {
            return Map.of();
        }
        if (keyStoreExists || settingsExist) {
            throw new IllegalStateException("Найдена неполная TLS-конфигурация в каталоге config.");
        }

        CertificateNames defaults = defaultNames();
        List<String> dnsNames = mergeNames(defaults.dnsNames(), environmentNames("BIKE_TLS_DNS_NAMES"));
        List<String> ipAddresses = mergeNames(defaults.ipAddresses(), environmentNames("BIKE_TLS_IP_ADDRESSES"));
        if (dnsNames.size() + ipAddresses.size() > 30) {
            throw new IllegalStateException("TLS-сертификат может содержать не более 30 имён и IP-адресов.");
        }
        String password = randomPassword();
        Path temporaryKeyStore = configDirectory.resolve(".server-tls-" + UUID.randomUUID() + ".p12");
        try {
            Files.createDirectories(configDirectory);
            generateKeyStore(temporaryKeyStore, password, dnsNames, ipAddresses);
            X509Certificate certificate = serverCertificate(loadKeyStore(temporaryKeyStore, password));
            validateCertificate(certificate);
            restrictAccess(temporaryKeyStore);
            replace(temporaryKeyStore, keyStorePath);
            writeSettings(password);
            log.info("Создан TLS-сертификат при запуске для {} DNS-имён и {} IP-адресов",
                    dnsNames.size(), ipAddresses.size());
            return Map.of(
                    "server.port", "8443",
                    "server.ssl.enabled", "true",
                    "server.ssl.key-store", "file:./config/server-tls.p12",
                    "server.ssl.key-store-password", password,
                    "server.ssl.key-store-type", "PKCS12",
                    "server.servlet.session.cookie.secure", "true");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Не удалось создать TLS-сертификат при запуске.", exception);
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Не удалось создать TLS-сертификат при запуске.", exception);
        } finally {
            try {
                Files.deleteIfExists(temporaryKeyStore);
            } catch (IOException ignored) {
                log.warn("Не удалось удалить временный keystore TLS-сертификата");
            }
        }
    }

    public synchronized TlsCertificateIssueResponse issue(TlsCertificateIssueRequest request) {
        if (Files.exists(keyStorePath) && Files.exists(settingsPath)) {
            try {
                X509Certificate activeCertificate = serverCertificate(loadKeyStore(
                        keyStorePath, readSettings().getProperty("server.ssl.key-store-password", "")));
                validateCertificate(activeCertificate);
                CertificateNames activeNames = certificateNames(activeCertificate);
                return new TlsCertificateIssueResponse(
                        "Действующий сертификат не заменён. Сертификат, созданный при следующем запуске, можно скачать кнопкой ниже.",
                        activeNames.dnsNames(), activeNames.ipAddresses(), fingerprint(activeCertificate));
            } catch (IOException | GeneralSecurityException exception) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Не удалось прочитать действующий сертификат сервера.", exception);
            }
        }
        CertificateNames defaults = defaultNames();
        List<String> dnsNames = mergeNames(defaults.dnsNames(), cleanNames(request.dnsNames(), "DNS-имя"));
        List<String> ipAddresses = mergeNames(defaults.ipAddresses(), cleanNames(request.ipAddresses(), "IP-адрес"));
        if (dnsNames.size() + ipAddresses.size() > 30) {
            throw badRequest("Сертификат может содержать не более 30 имён и IP-адресов.");
        }

        String password = randomPassword();
        Path temporaryKeyStore = configDirectory.resolve(".server-tls-" + UUID.randomUUID() + ".p12");
        try {
            Files.createDirectories(configDirectory);
            generateKeyStore(temporaryKeyStore, password, dnsNames, ipAddresses);
            KeyStore keyStore = loadKeyStore(temporaryKeyStore, password);
            X509Certificate certificate = serverCertificate(keyStore);
            validateCertificate(certificate);
            restrictAccess(temporaryKeyStore);
            replace(temporaryKeyStore, keyStorePath);
            writeSettings(password);
            log.info("Выпущен TLS-сертификат для {} DNS-имён и {} IP-адресов",
                    dnsNames.size(), ipAddresses.size());
            return new TlsCertificateIssueResponse(
                    "Сертификат создан. Он станет активным после перезапуска сервера.",
                    dnsNames, ipAddresses, fingerprint(certificate));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось выпустить сертификат. Проверьте, что сервер запущен на JDK 21.", exception);
        } catch (IOException | GeneralSecurityException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось выпустить сертификат. Проверьте, что сервер запущен на JDK 21.", exception);
        } finally {
            try {
                Files.deleteIfExists(temporaryKeyStore);
            } catch (IOException ignored) {
                log.warn("Не удалось удалить временный keystore TLS-сертификата");
            }
        }
    }

    public synchronized String upload(MultipartFile upload, String password) {
        if (upload == null || upload.isEmpty() || upload.getSize() > MAX_KEYSTORE_BYTES) {
            throw badRequest("Выберите файл PKCS#12 размером до 5 МБ.");
        }
        if (password == null || password.isBlank()) {
            throw badRequest("Введите пароль файла PKCS#12.");
        }
        Path temporaryKeyStore = configDirectory.resolve(".server-tls-" + UUID.randomUUID() + ".p12");
        try {
            Files.createDirectories(configDirectory);
            Files.write(temporaryKeyStore, upload.getBytes());
            restrictAccess(temporaryKeyStore);
            X509Certificate certificate = serverCertificate(loadKeyStore(temporaryKeyStore, password));
            validateCertificate(certificate);
            replace(temporaryKeyStore, keyStorePath);
            writeSettings(password);
            log.info("Загружен TLS-keystore PKCS#12 с действующим сертификатом");
            return fingerprint(certificate);
        } catch (IOException | GeneralSecurityException exception) {
            throw badRequest("Не удалось прочитать PKCS#12. Проверьте файл, пароль и срок сертификата.", exception);
        } finally {
            try {
                Files.deleteIfExists(temporaryKeyStore);
            } catch (IOException ignored) {
                log.warn("Не удалось удалить временный загруженный keystore TLS-сертификата");
            }
        }
    }

    public synchronized PublicCertificate downloadPublicCertificate() {
        if (!Files.exists(keyStorePath) || !Files.exists(settingsPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сертификат сервера ещё не выпущен.");
        }
        Properties settings = readSettings();
        String password = settings.getProperty("server.ssl.key-store-password", "");
        try {
            X509Certificate certificate = serverCertificate(loadKeyStore(keyStorePath, password));
            String pem = "-----BEGIN CERTIFICATE-----\n"
                    + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(certificate.getEncoded())
                    + "\n-----END CERTIFICATE-----\n";
            return new PublicCertificate(
                    pem.getBytes(java.nio.charset.StandardCharsets.US_ASCII), fingerprint(certificate));
        } catch (IOException | GeneralSecurityException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось прочитать сертификат сервера.", exception);
        }
    }

    private String fingerprint(X509Certificate certificate) throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        return java.util.HexFormat.ofDelimiter(":").withUpperCase().formatHex(digest);
    }

    private void generateKeyStore(Path destination, String password,
                                  List<String> dnsNames, List<String> ipAddresses)
            throws IOException, InterruptedException {
        List<String> names = new ArrayList<>();
        dnsNames.forEach(name -> names.add("dns:" + name));
        ipAddresses.forEach(address -> names.add("ip:" + address));
        String distinguishedName = "CN=" + (dnsNames.isEmpty() ? ipAddresses.getFirst() : dnsNames.getFirst());
        String keyTool = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "keytool.exe" : "keytool")
                .toString();
        ProcessBuilder builder = new ProcessBuilder(
                keyTool, "-genkeypair", "-alias", "bike-server", "-keyalg", "RSA", "-keysize", "3072",
                "-storetype", "PKCS12", "-keystore", destination.toString(), "-validity", "825",
                "-dname", distinguishedName, "-ext", "SAN=" + String.join(",", names),
                "-storepass:env", "BIKE_TLS_KEYSTORE_PASSWORD", "-noprompt");
        builder.redirectErrorStream(true);
        builder.environment().put("BIKE_TLS_KEYSTORE_PASSWORD", password);
        Process process = builder.start();
        byte[] output = process.getInputStream().readAllBytes();
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("keytool timed out");
        }
        if (process.exitValue() != 0) {
            String detail = new String(output, java.nio.charset.StandardCharsets.UTF_8).strip();
            throw new IOException("keytool failed: " + detail);
        }
    }

    private KeyStore loadKeyStore(Path path, String password) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(path)) {
            keyStore.load(input, password.toCharArray());
        }
        return keyStore;
    }

    private X509Certificate serverCertificate(KeyStore keyStore) throws GeneralSecurityException {
        var aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!keyStore.isKeyEntry(alias)) continue;
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate instanceof X509Certificate x509) return x509;
        }
        throw badRequest("В PKCS#12 нет закрытого ключа и серверного X.509 сертификата.");
    }

    private CertificateNames certificateNames(X509Certificate certificate) throws GeneralSecurityException {
        List<String> dnsNames = new ArrayList<>();
        List<String> ipAddresses = new ArrayList<>();
        var alternativeNames = certificate.getSubjectAlternativeNames();
        if (alternativeNames != null) {
            for (List<?> entry : alternativeNames) {
                if (entry.size() < 2 || !(entry.get(0) instanceof Integer type)) continue;
                if (type == 2 && entry.get(1) instanceof String name) dnsNames.add(name);
                if (type == 7 && entry.get(1) != null) ipAddresses.add(entry.get(1).toString());
            }
        }
        return new CertificateNames(List.copyOf(dnsNames), List.copyOf(ipAddresses));
    }

    private void validateCertificate(X509Certificate certificate) throws GeneralSecurityException {
        try {
            certificate.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException exception) {
            throw badRequest("Сертификат истёк или ещё не вступил в силу.", exception);
        }
        if (certificate.getSubjectAlternativeNames() == null) {
            throw badRequest("В сертификате должны быть заданы DNS-имена или IP-адреса в SAN.");
        }
    }

    private List<String> cleanNames(List<String> values, String label) {
        if (values == null) return List.of();
        List<String> result = values.stream().map(String::trim).filter(value -> !value.isEmpty())
                .distinct().toList();
        if (result.stream().anyMatch(value -> value.length() > 253 || value.contains(",") || value.contains(" "))) {
            throw badRequest("Проверьте формат поля «" + label + "».");
        }
        return result;
    }

    private List<String> environmentNames(String property) {
        String value = System.getenv(property);
        if (value == null || value.isBlank()) return List.of();
        return cleanNames(Arrays.stream(value.split(",")).toList(), property);
    }

    private CertificateNames defaultNames() {
        List<String> dnsNames = new ArrayList<>(List.of("localhost"));
        List<String> ipAddresses = new ArrayList<>(List.of("127.0.0.1", "::1"));
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            addName(dnsNames, localHost.getHostName());
            addName(dnsNames, localHost.getCanonicalHostName());
        } catch (IOException ignored) {
            log.debug("Не удалось определить DNS-имя компьютера для SAN сертификата");
        }
        detectLocalIpv4Addresses().forEach(address -> addName(ipAddresses, address));
        return new CertificateNames(dnsNames, ipAddresses);
    }

    public List<String> detectLocalIpv4Addresses() {
        List<String> addresses = new ArrayList<>();
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback()) continue;
                var networkAddresses = network.getInetAddresses();
                while (networkAddresses.hasMoreElements()) {
                    InetAddress address = networkAddresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress()) {
                        addName(addresses, address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
            log.debug("Не удалось определить сетевые IP-адреса сервера");
        }
        return List.copyOf(addresses);
    }

    private void addName(List<String> names, String value) {
        if (value != null && !value.isBlank() && !value.equalsIgnoreCase("localhost") && !names.contains(value)) {
            names.add(value);
        }
    }

    private List<String> mergeNames(List<String> defaults, List<String> requested) {
        List<String> names = new ArrayList<>(defaults);
        requested.stream().filter(value -> !names.contains(value)).forEach(names::add);
        return names;
    }

    private record CertificateNames(List<String> dnsNames, List<String> ipAddresses) {
    }

    public record PublicCertificate(byte[] content, String sha256Fingerprint) {
    }

    private String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(40);
        for (int index = 0; index < 40; index++) {
            password.append(PASSWORD_CHARACTERS[random.nextInt(PASSWORD_CHARACTERS.length)]);
        }
        return password.toString();
    }

    private void writeSettings(String password) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("server.port", "8443");
        properties.setProperty("server.ssl.enabled", "true");
        properties.setProperty("server.ssl.key-store", "file:./config/server-tls.p12");
        properties.setProperty("server.ssl.key-store-password", password);
        properties.setProperty("server.ssl.key-store-type", "PKCS12");
        properties.setProperty("server.servlet.session.cookie.secure", "true");
        Path temporary = Files.createTempFile(configDirectory, ".server-tls-", ".tmp");
        try {
            restrictAccess(temporary);
            try (var output = Files.newOutputStream(temporary)) {
                properties.store(output, "TLS settings. Contains the keystore password; do not commit.");
            }
            replace(temporary, settingsPath);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Properties readSettings() {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(settingsPath)) {
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось прочитать TLS-настройки.", exception);
        }
    }

    private void replace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void restrictAccess(Path path) throws IOException {
        var posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null) posix.setPermissions(PosixFilePermissions.fromString("rw-------"));
        var acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
            acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                    .setPrincipal(acl.getOwner()).setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException badRequest(String message, Exception cause) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message, cause);
    }
}
