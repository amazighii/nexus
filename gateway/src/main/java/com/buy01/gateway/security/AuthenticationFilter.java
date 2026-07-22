package com.buy01.gateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component

public class AuthenticationFilter implements GlobalFilter, Ordered {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Autowired
        private JwtUtils jwtUtil;

        private static final List<String> PUBLIC_PATHS = List.of(
                        // swagger docs and auth routes
                        "/v3/api-docs",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/actuator/health");

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                String path = exchange.getRequest().getPath().toString();

                // Skip auth for public routes
                if (isPublic(exchange.getRequest().getMethod(), path)) {
                        System.out.println("Public path accessed: " + path);
                        return chain.filter(exchange);
                }
                exchange.getRequest().getHeaders().forEach((k, v) -> System.out.println("[ " + k + ": " + v + " ]"));
                String authHeader = exchange.getRequest()
                                .getHeaders()
                                .getFirst(HttpHeaders.AUTHORIZATION);

                // No token at all → 401
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        return writeError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized",
                                        "Authentication token is missing or malformed.");
                }

                String token = authHeader.substring(7);

                // Invalid or expired token → 401
                if (!jwtUtil.isTokenValid(token)) {
                        return writeError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized",
                                        "Authentication token is invalid or expired.");
                }

                // Token is valid — extract claims and add headers
                String userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);

                // Mutate the request to add the headers before forwarding
                ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(exchange.getRequest().mutate()
                                                .header("X-User-Id", userId)
                                                .header("X-User-Role", role)
                                                .build())
                                .build();

                return chain.filter(mutatedExchange);
        }

        @Override
        public int getOrder() {
                return -1; // Run before all other filters
        }

        private boolean isPublic(HttpMethod method, String path) {
                return PUBLIC_PATHS.stream().anyMatch(path::startsWith)
                                || HttpMethod.GET.equals(method) && path.startsWith("/api/products");
        }

        private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String error, String message) {
                exchange.getResponse().setStatusCode(status);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = Map.of(
                                "timestamp", Instant.now().toString(),
                                "status", status.value(),
                                "error", error,
                                "message", message);

                return exchange.getResponse().writeWith(Mono.fromCallable(() -> exchange.getResponse().bufferFactory()
                                .wrap(objectMapper.writeValueAsBytes(body))));
        }
}
