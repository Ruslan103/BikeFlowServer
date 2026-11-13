package com.example.demo.dto.settings;

import java.util.List;

public record ServerAddressesResponse(List<String> localBaseUrls, String publicBaseUrl) {
}
