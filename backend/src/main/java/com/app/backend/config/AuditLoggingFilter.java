package com.app.backend.config;

import com.app.backend.entity.AuditLog;
import com.app.backend.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Persists a lightweight row per successful mutating {@code /api/**} call (when enabled).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditService auditService;

    @Value("${app.audit.enabled:true}")
    private boolean auditEnabled;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (!auditEnabled || response.getStatus() >= 400) {
            return;
        }
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) {
            return;
        }
        String method = request.getMethod();
        if (!isMutation(method)) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setOccurredAt(Instant.now());
        log.setHttpMethod(method);
        log.setRequestPath(uri.length() > 500 ? uri.substring(0, 500) : uri);
        log.setStatusCode(response.getStatus());
        log.setClientIp(clientIp(request));
        fillActor(log);
        auditService.recordMutation(log);
    }

    private static boolean isMutation(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private static String clientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            int comma = xf.indexOf(',');
            return comma > 0 ? xf.substring(0, comma).trim() : xf.trim();
        }
        return request.getRemoteAddr();
    }

    private static void fillActor(AuditLog log) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }
        Object p = auth.getPrincipal();
        if (p instanceof com.app.backend.security.UserPrincipal up) {
            log.setActorUserId(up.getId());
            log.setActorEmail(up.getEmail());
        }
    }
}
