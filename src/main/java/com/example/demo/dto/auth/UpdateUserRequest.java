package com.example.demo.dto.auth;
import jakarta.validation.constraints.*;
public record UpdateUserRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,100}") String login,
        @Size(max = 200) String displayName
) {}
