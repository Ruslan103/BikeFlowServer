package com.example.setup;

import com.example.demo.dto.auth.CreateUserRequest;
import jakarta.servlet.DispatcherType;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class SetupSecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        log.debug("Настраивается кодировщик пароля для режима первоначальной настройки");
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    UserDetailsService setupUsers(Environment environment, PasswordEncoder encoder, Validator validator) {
        log.info("Настраивается временная учётная запись администратора");
        String login = environment.getProperty("BIKE_ADMIN_LOGIN");
        String password = environment.getProperty("BIKE_ADMIN_PASSWORD");
        var request = new CreateUserRequest(login, "Administrator", password, Set.of("ADMIN"), true);
        if (!validator.validate(request).isEmpty()) {
            throw new IllegalStateException(
                    "Setup requires BIKE_ADMIN_LOGIN and BIKE_ADMIN_PASSWORD (12-128 characters)");
        }
        return new InMemoryUserDetailsManager(User.withUsername(login)
                .password(encoder.encode(password)).roles("ADMIN").build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService details, PasswordEncoder encoder) {
        log.debug("Настраивается аутентификация для режима первоначальной настройки");
        var provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        log.debug("Настраивается сессия режима первоначальной настройки");
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        log.debug("Настраиваются CSRF-токены режима первоначальной настройки");
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
                                           CsrfTokenRepository csrfTokens) throws Exception {
        log.info("Настраиваются правила безопасности кабинета первоначальной настройки");
        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(contexts))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
                .authorizeHttpRequests(access -> access
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/login", "/admin-assets/**", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/admin", "/admin/logs", "/api/admin/settings", "/api/admin/restart",
                                "/api/admin/logs", "/api/admin/shutdown", "/api/auth/logout").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            if ("/admin".equals(request.getRequestURI())) {
                                response.sendRedirect("/login");
                            } else {
                                response.setStatus(401);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"status\":401,\"detail\":\"Authentication required\"}");
                            }
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"detail\":\"Access denied or invalid CSRF token\"}");
                        }))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true).deleteCookies("BIKE_SETUP_SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        return http.build();
    }
}
