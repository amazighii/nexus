package com.buy01.orders.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class IsAuthorized extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // @Override
    // protected boolean shouldNotFilter(HttpServletRequest request) {
    //     String path = request.getRequestURI();
    //     return path.startsWith("/v3/api-docs")
    //             || path.startsWith("/swagger-ui")
    //             || path.equals("/swagger-ui.html")
    //             || path.equals("/actuator/health");
    // }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws IOException, ServletException {

        if (!request.getRequestURI().startsWith("/api/orders")) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userId == null || userRole == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "Authentication is required to access order resources.");
            return;
        }

        // if (!userRole.equals("SELLER")) {
        //     writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
        //             "Only sellers can perform this media action.");
        //     return;
        // }

        filterChain.doFilter(request, response);
    }


    private void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "error", error,
                "message", message));
    }
}
