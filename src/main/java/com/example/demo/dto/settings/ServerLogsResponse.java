package com.example.demo.dto.settings;

import java.util.List;

public record ServerLogsResponse(boolean available, List<String> lines) {
}
