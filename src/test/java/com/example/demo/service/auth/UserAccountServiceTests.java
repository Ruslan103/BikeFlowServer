package com.example.demo.service.auth;

import com.example.demo.dto.auth.UpdateRolesRequest;
import com.example.demo.model.auth.Role;
import com.example.demo.model.auth.UserAccount;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserAccountRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserAccountServiceTests {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final JdbcIndexedSessionRepository sessions = mock(JdbcIndexedSessionRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserAccountServiceImpl service = new UserAccountServiceImpl(
            users, roles, encoder, sessions);

    @Test
    void passwordResetHashesPasswordAndRevokesSessions() {
        UserAccount user = admin();
        when(encoder.encode("new password value")).thenReturn("encoded-password");
        when(sessions.findByPrincipalName("1")).thenReturn(java.util.Map.of());
        service.resetPassword(1L, "new password value");
        assertEquals("encoded-password", user.getPasswordHash());
        assertEquals(1, user.getSecurityVersion());
        verify(sessions).findByPrincipalName("1");
    }

    @Test
    void lastActiveAdminCannotBeDisabled() {
        UserAccount user = admin();
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.updateEnabled(1L, false));
        assertEquals(409, exception.getStatusCode().value());
        assertTrue(user.isEnabled());
        verifyNoInteractions(sessions);
    }

    @Test
    void lastActiveAdminCannotLoseAdminRole() {
        UserAccount user = admin();
        Role manager = mock(Role.class);
        when(manager.getCode()).thenReturn("MANAGER");
        when(roles.findByCode("MANAGER")).thenReturn(Optional.of(manager));
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.updateRoles(1L, new UpdateRolesRequest(Set.of("MANAGER"))));
        assertEquals(409, exception.getStatusCode().value());
        assertEquals("ADMIN", user.getRoles().iterator().next().getCode());
    }

    private UserAccount admin() {
        Role role = mock(Role.class);
        when(role.getCode()).thenReturn("ADMIN");
        when(roles.lockAdminRole()).thenReturn(Optional.of(role));
        when(users.countByEnabledTrueAndRolesCode("ADMIN")).thenReturn(1L);
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setRoles(Set.of(role));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        return user;
    }
}
