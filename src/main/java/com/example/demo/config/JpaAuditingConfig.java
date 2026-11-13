package com.example.demo.config;

import com.example.demo.model.auth.UserAccount;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")
@Slf4j
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<UserAccount> currentUserAuditor(EntityManager entityManager) {
        log.debug("Настраивается определение пользователя для аудита изменений");
        return () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            }
            Long userId = Long.valueOf(authentication.getName());
            return Optional.of(entityManager.getReference(UserAccount.class, userId));
        };
    }
}
