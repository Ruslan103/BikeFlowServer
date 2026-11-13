package com.example.demo.controller;

import com.example.demo.dto.settings.TlsCertificateIssueRequest;
import com.example.demo.dto.settings.TlsCertificateIssueResponse;
import com.example.demo.service.settings.TlsCertificateService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/tls")
@RequiredArgsConstructor
@Slf4j
public class TlsCertificateController {
    private final TlsCertificateService certificates;

    @PostMapping("/certificate/issue")
    public ResponseEntity<TlsCertificateIssueResponse> issue(@Valid @RequestBody TlsCertificateIssueRequest request) {
        return ResponseEntity.accepted().body(certificates.issue(request));
    }

    @GetMapping("/certificate")
    public ResponseEntity<byte[]> download() {
        TlsCertificateService.PublicCertificate certificate = certificates.downloadPublicCertificate();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("application/x-pem-file"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("server-tls.crt").build().toString())
                .header("X-Certificate-SHA256", certificate.sha256Fingerprint())
                .body(certificate.content());
    }

    @PostMapping(value = "/certificate/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestPart("certificate") MultipartFile certificate,
            @RequestParam("password") String password) {
        String fingerprint = certificates.upload(certificate, password);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "Сертификат сервера загружен. Перезапустите сервер, чтобы применить его.",
                "sha256Fingerprint", fingerprint));
    }
}
