package com.example.demo.service.auth;

import com.example.demo.dto.auth.*;
import org.springframework.data.domain.Page;

public interface UserAccountService {
    UserResponse create(CreateUserRequest request);
    Page<UserResponse> findAll(String search, Boolean enabled, int page, int size);
    UserResponse findById(Long id);
    UserResponse update(Long id, UpdateUserRequest request);
    UserResponse updateRoles(Long id, UpdateRolesRequest request);
    UserResponse updateEnabled(Long id, boolean enabled);
    void changePassword(Long id, ChangePasswordRequest request);
    void resetPassword(Long id, String password);
    void logoutAll(Long id);
    void bootstrap(String login, String password);
}
