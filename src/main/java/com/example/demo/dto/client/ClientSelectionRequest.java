package com.example.demo.dto.client;

public record ClientSelectionRequest(@jakarta.validation.constraints.NotNull Long clientId, Long bicycleId, Long contactId) { }
