package com.example.demo.dto.auth;
import jakarta.validation.constraints.*;
import java.util.Set;
public record CreateUserRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,100}") String login,
        @Size(max = 200) String displayName,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotEmpty Set<@NotBlank String> roleCodes,
        boolean enabled
) {}
