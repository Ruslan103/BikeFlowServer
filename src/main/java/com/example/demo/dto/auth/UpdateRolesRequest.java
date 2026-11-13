package com.example.demo.dto.auth;
import jakarta.validation.constraints.*;
import java.util.Set;
public record UpdateRolesRequest(@NotEmpty Set<@NotBlank String> roleCodes) {}
