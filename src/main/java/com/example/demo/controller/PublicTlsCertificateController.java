package com.example.demo.controller;

import com.example.demo.service.settings.TlsCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publishes only the public certificate so a client can bootstrap trust. */
@RestController
@RequestMapping("/api/tls")
@RequiredArgsConstructor
public class PublicTlsCertificateController {
    private final TlsCertificateService certificates;

    @GetMapping(value = "/certificate", produces = "application/x-pem-file")
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
}
