package com.example.demo.dto.settings;

import java.util.List;

public record TlsCertificateIssueResponse(
        String message, List<String> dnsNames, List<String> ipAddresses, String sha256Fingerprint) {
}
