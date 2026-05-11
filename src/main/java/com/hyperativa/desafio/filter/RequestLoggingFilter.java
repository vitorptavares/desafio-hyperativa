package com.hyperativa.desafio.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Loga inicio e fim de cada requisicao.
 * Corpo nunca e logado para nao vazar dados sensiveis (numeros de cartao).
 */
@Component
@Order(-50)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_KEY = "correlationId";
    private static final String USER_KEY = "user";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_KEY, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        long start = System.currentTimeMillis();
        String uri = buildUri(request);
        String clientIp = resolveClientIp(request);

        log.info("--> {} {} ip={}", request.getMethod(), uri, clientIp);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String user = resolveUser();
            MDC.put(USER_KEY, user);
            log.info("<-- {} {} user={} status={} {}ms",
                    request.getMethod(), uri, user, response.getStatus(), elapsed);
            MDC.remove(USER_KEY);
            MDC.remove(CORRELATION_KEY);
        }
    }

    private String buildUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return query != null ? uri + "?" + query : uri;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
