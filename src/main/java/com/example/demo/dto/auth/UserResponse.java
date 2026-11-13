package com.example.demo.dto.auth;
import java.time.Instant;
import java.util.Set;
public record UserResponse(Long id, String login, String displayName, Set<String> roleCodes,
                           boolean enabled, Instant createdAt) {}
