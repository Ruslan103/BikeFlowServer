package com.example.demo.dto.auth;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Size(min = 12, max = 128) String newPassword) {}
