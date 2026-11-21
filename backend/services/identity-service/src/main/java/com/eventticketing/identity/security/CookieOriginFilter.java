package com.eventticketing.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.filter.OncePerRequestFilter;

/** Security Design §5.3/§7: refresh cookies cannot authorize cross-origin POSTs. */
public class CookieOriginFilter extends OncePerRequestFilter {
    private final Set<String> origins;
    public CookieOriginFilter(String origins) {
        this.origins = Arrays.stream(origins.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI();
        if ("POST".equals(request.getMethod()) && Set.of("/api/v1/auth/refresh", "/api/v1/auth/logout").contains(path)
                && (request.getHeader("Origin") == null || !origins.contains(request.getHeader("Origin")))) {
            response.setStatus(403); response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":403,\"code\":\"ORIGIN_NOT_ALLOWED\",\"detail\":\"A trusted browser origin is required.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
