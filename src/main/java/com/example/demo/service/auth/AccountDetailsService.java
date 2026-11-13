package com.example.demo.service.auth;

import com.example.demo.model.auth.Role;
import com.example.demo.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDetailsService implements UserDetailsService {
    private final UserAccountRepository users;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        log.info("Выполняется поиск учётной записи для аутентификации");
        var account = users.findByLoginIgnoreCase(login)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        log.info("Учётная запись для аутентификации найдена");
        return User.withUsername(account.getId().toString())
                .password(account.getPasswordHash())
                .disabled(!account.isEnabled())
                .roles(account.getRoles().stream().map(Role::getCode).toArray(String[]::new))
                .build();
    }
}
