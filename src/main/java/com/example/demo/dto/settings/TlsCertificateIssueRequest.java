package com.example.demo.dto.settings;

import jakarta.validation.constraints.Size;
import java.util.List;

public record TlsCertificateIssueRequest(
        @Size(max = 30) List<String> dnsNames,
        @Size(max = 30) List<String> ipAddresses) {
}
