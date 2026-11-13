package com.example.demo.security;

import com.example.demo.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class AccountSessionFilter extends OncePerRequestFilter {
    public static final String VERSION_ATTRIBUTE = "accountSecurityVersion";
    private final UserAccountRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        log.debug("Проверяется актуальность пользовательской сессии");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            var account = users.findById(Long.valueOf(authentication.getName()));
            var session = request.getSession(false);
            Object version = session == null ? null : session.getAttribute(VERSION_ATTRIBUTE);
            if (account.isEmpty() || !account.get().isEnabled()
                    || !Long.valueOf(account.get().getSecurityVersion()).equals(version)) {
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"detail\":\"Session has expired\"}");
                log.warn("Сессия отклонена: учётная запись отключена или данные безопасности устарели");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
