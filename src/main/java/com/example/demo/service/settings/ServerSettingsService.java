package com.example.demo.service.settings;

import com.example.demo.dto.settings.ServerSettingsRequest;
import com.example.demo.dto.settings.ServerSettingsResponse;

public interface ServerSettingsService {
    ServerSettingsResponse read();
    ServerSettingsResponse save(ServerSettingsRequest request, Long adminUserId);
}
