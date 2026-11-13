package com.example.demo.security;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.ApiExceptionHandler;
import com.example.demo.controller.AuthController;
import com.example.demo.controller.UserAccountController;
import com.example.demo.controller.AdminPageController;
import com.example.demo.controller.ServerSettingsController;
import com.example.demo.dto.settings.ServerSettingsResponse;
import com.example.demo.service.settings.ServerSettingsService;
import com.example.demo.dto.auth.UserResponse;
import com.example.demo.service.auth.UserAccountService;
import com.example.demo.model.auth.Role;
import com.example.demo.model.auth.UserAccount;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserAccountRepository;
import com.example.demo.service.auth.AccountDetailsService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig({SecurityConfig.class, AuthController.class, UserAccountController.class,
        AdminPageController.class, ServerSettingsController.class,
        ApiExceptionHandler.class, SecurityApiTests.TestConfig.class})
@WebAppConfiguration
class SecurityApiTests {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {
        @Bean UserAccountRepository users() { return mock(UserAccountRepository.class); }
        @Bean RoleRepository roles() { return mock(RoleRepository.class); }
        @Bean AccountDetailsService details() { return mock(AccountDetailsService.class); }
        @Bean UserAccountService service() { return mock(UserAccountService.class); }
        @Bean ServerSettingsService settings() { return mock(ServerSettingsService.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired UserAccountRepository users;
    @Autowired RoleRepository roles;
    @Autowired AccountDetailsService details;
    @Autowired UserAccountService service;
    @Autowired ServerSettingsService settings;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    UserAccount account;

    @BeforeEach
    void setUp() {
        reset(users, roles, details, service, settings);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        account = new UserAccount();
        account.setId(1L);
        account.setEnabled(true);
        when(users.findById(1L)).thenReturn(Optional.of(account));
        when(roles.lockAdminRole()).thenReturn(Optional.of(mock(Role.class)));
        when(service.findById(1L)).thenReturn(
                new UserResponse(1L, "admin", "Admin", Set.of("ADMIN"), true, null));
    }

    @Test
    void anonymousCannotReadAccount() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminPageRedirectsToPublicLoginAndSettingsRemainPrivate() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminCanReadSettings() throws Exception {
        when(settings.read()).thenReturn(new ServerSettingsResponse(
                "jdbc:postgresql://localhost/bike", "postgres", true, true, true, false, false, false));
        mvc.perform(get("/api/admin/settings").with(user("1").roles("MANAGER"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/settings").with(user("1").roles("ADMIN"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trelloToken").doesNotExist())
                .andExpect(jsonPath("$.databasePassword").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void loginRequiresCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"login\":\"admin\",\"password\":\"a long password\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(details);
    }

    @Test
    void csrfEndpointProvidesTokenAcceptedByLogin() throws Exception {
        var csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        var token = (org.springframework.security.web.csrf.CsrfToken)
                csrfResult.getRequest().getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        when(details.loadUserByUsername("admin")).thenReturn(User.withUsername("1")
                .password(encoder.encode("a long password")).roles("ADMIN").build());
        mvc.perform(post("/api/auth/login")
                .session((MockHttpSession) csrfResult.getRequest().getSession(false))
                .header(token.getHeaderName(), token.getToken())
                .contentType("application/json")
                .content("{\"login\":\"admin\",\"password\":\"a long password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPersistsSessionAndAllowsNextRequest() throws Exception {
        when(details.loadUserByUsername("admin")).thenReturn(User.withUsername("1")
                .password(encoder.encode("a long password")).roles("ADMIN").build());
        var result = mvc.perform(post("/api/auth/login").with(csrf())
                .contentType("application/json")
                .content("{\"login\":\"admin\",\"password\":\"a long password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("admin"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/users/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }

    @Test
    void invalidPasswordReturnsUnauthorized() throws Exception {
        when(details.loadUserByUsername("admin")).thenReturn(User.withUsername("1")
                .password(encoder.encode("another password")).roles("ADMIN").build());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                .content("{\"login\":\"admin\",\"password\":\"a long password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedSessionIsRejected() throws Exception {
        account.setSecurityVersion(1);
        mvc.perform(get("/api/users/me").with(user("1").roles("ADMIN"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAccountIsRejected() throws Exception {
        account.setEnabled(false);
        mvc.perform(get("/api/users/me").with(user("1").roles("ADMIN"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managerCannotManageUsers() throws Exception {
        mvc.perform(get("/api/users").with(user("1").roles("MANAGER"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotReadOrdersOrOwnAccount() throws Exception {
        for (String path : new String[]{"/api/work-orders", "/api/users/me"}) {
            mvc.perform(get(path).with(user("1").roles("MANAGER"))
                    .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminCanListUsers() throws Exception {
        when(service.findAll(isNull(), isNull(), eq(0), eq(20))).thenReturn(Page.empty());
        mvc.perform(get("/api/users").with(user("1").roles("ADMIN"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L))
                .andExpect(status().isOk());
    }

    @Test
    void mechanicCannotUpdateOrders() throws Exception {
        mvc.perform(put("/api/work-orders/00001").with(user("1").roles("MECHANIC"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L).with(csrf()))
                .andExpect(status().isForbidden());
    }
    @Test
    void trelloRequiresAdminAndCsrf() throws Exception {
        mvc.perform(get("/api/trello/boards")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trello/boards").with(user("1").roles("MANAGER"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/work-orders/00001/trello/card").with(user("1").roles("ADMIN"))
                .sessionAttr(AccountSessionFilter.VERSION_ATTRIBUTE, 0L)
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }
}
