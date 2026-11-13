package com.example.demo.dto.auth;

import java.util.List;

public record UserPageResponse(List<UserResponse> content, int page, int size,
                               long totalElements, int totalPages) {
}
