package com.example.demo.service.auth;

import com.example.demo.dto.auth.*;
import com.example.demo.model.auth.Role;
import com.example.demo.model.auth.UserAccount;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserAccountRepository;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserAccountServiceImpl implements UserAccountService {
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JdbcIndexedSessionRepository sessions;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        log.info("Начато создание учётной записи");
        lockAccounts();
        checkLogin(request.login(), null);
        UserAccount user = new UserAccount();
        user.setLogin(normalize(request.login()));
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(resolveRoles(request.roleCodes()));
        user.setEnabled(request.enabled());
        UserResponse result = response(users.saveAndFlush(user));
        log.info("Учётная запись успешно создана");
        return result;
    }

    @Override
    public Page<UserResponse> findAll(String search, Boolean enabled, int page, int size) {
        log.info("Начато получение списка учётных записей");
        if (page < 0 || size < 1 || size > 100) {
            throw error(HttpStatus.BAD_REQUEST, "Page must be non-negative and size between 1 and 100");
        }
        Specification<UserAccount> specification = (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (enabled != null) {
                predicate = builder.and(predicate, builder.equal(root.get("enabled"), enabled));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("login")), pattern, '\\'),
                        builder.like(builder.lower(root.get("displayName")), pattern, '\\')));
            }
            return predicate;
        };
        return users.findAll(specification, PageRequest.of(page, size, Sort.by("id")))
                .map(this::response);
    }

    @Override
    public UserResponse findById(Long id) {
        log.info("Начато получение учётной записи");
        return response(find(id));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        log.info("Начато обновление учётной записи");
        lockAccounts();
        UserAccount user = find(id);
        checkLogin(request.login(), id);
        user.setLogin(normalize(request.login()));
        user.setDisplayName(request.displayName());
        UserResponse result = response(user);
        log.info("Учётная запись успешно обновлена");
        return result;
    }

    @Override
    @Transactional
    public UserResponse updateRoles(Long id, UpdateRolesRequest request) {
        log.info("Начато изменение ролей учётной записи");
        lockAccounts();
        UserAccount user = find(id);
        Set<Role> newRoles = resolveRoles(request.roleCodes());
        protectLastAdmin(user, user.isEnabled(), newRoles);
        user.setRoles(newRoles);
        revoke(user);
        UserResponse result = response(user);
        log.info("Роли учётной записи успешно изменены");
        return result;
    }

    @Override
    @Transactional
    public UserResponse updateEnabled(Long id, boolean enabled) {
        log.info("Начато изменение активности учётной записи");
        lockAccounts();
        UserAccount user = find(id);
        protectLastAdmin(user, enabled, user.getRoles());
        user.setEnabled(enabled);
        revoke(user);
        UserResponse result = response(user);
        log.info("Активность учётной записи успешно изменена");
        return result;
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        log.info("Начата смена пароля учётной записи");
        lockAccounts();
        UserAccount user = find(id);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw error(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        revoke(user);
        log.info("Пароль учётной записи успешно изменён");
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String password) {
        log.info("Начат сброс пароля учётной записи");
        lockAccounts();
        UserAccount user = find(id);
        user.setPasswordHash(passwordEncoder.encode(password));
        revoke(user);
        log.info("Пароль учётной записи успешно сброшен");
    }

    @Override
    @Transactional
    public void logoutAll(Long id) {
        log.info("Начато завершение сессий учётной записи");
        lockAccounts();
        revoke(find(id));
        log.info("Сессии учётной записи завершены");
    }

    @Override
    @Transactional
    public void bootstrap(String login, String password) {
        log.info("Проверяется необходимость создания администратора при запуске");
        lockAccounts();
        if (users.count() != 0) {
            log.info("Администратор уже существует, начальное создание пропущено");
            return;
        }
        create(new CreateUserRequest(login, "Administrator", password, Set.of("ADMIN"), true));
        log.info("Начальная учётная запись администратора создана");
    }

    private void lockAccounts() {
        roles.lockAdminRole().orElseThrow(() ->
                error(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN role is not configured"));
    }

    private UserAccount find(Long id) {
        return users.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void checkLogin(String login, Long currentId) {
        users.findByLoginIgnoreCase(normalize(login)).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw error(HttpStatus.CONFLICT, "Login is already taken");
            }
        });
    }

    private Set<Role> resolveRoles(Set<String> codes) {
        Set<Role> result = new HashSet<>();
        for (String code : codes) {
            result.add(roles.findByCode(code).orElseThrow(() ->
                    error(HttpStatus.BAD_REQUEST, "Unknown role: " + code)));
        }
        return result;
    }

    private void protectLastAdmin(UserAccount user, boolean enabled, Set<Role> newRoles) {
        boolean wasAdmin = user.isEnabled() && hasAdmin(user.getRoles());
        boolean remainsAdmin = enabled && hasAdmin(newRoles);
        if (wasAdmin && !remainsAdmin && users.countByEnabledTrueAndRolesCode("ADMIN") <= 1) {
            throw error(HttpStatus.CONFLICT, "Cannot disable or remove the last active administrator");
        }
    }

    private boolean hasAdmin(Set<Role> assignedRoles) {
        return assignedRoles.stream().anyMatch(role -> "ADMIN".equals(role.getCode()));
    }

    private void revoke(UserAccount user) {
        user.setSecurityVersion(user.getSecurityVersion() + 1);
        sessions.findByPrincipalName(user.getId().toString()).keySet().forEach(sessions::deleteById);
    }

    private UserResponse response(UserAccount user) {
        return new UserResponse(user.getId(), user.getLogin(), user.getDisplayName(),
                user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()),
                user.isEnabled(), user.getCreatedAt());
    }

    private String normalize(String login) {
        return login.toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
