package com.buy01.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder) {
                return builder.routes()

                                .route("user-service", r -> r
                                                .path("/api/auth/**", "/api/users/**")
                                                .uri("lb://USER-SERVICE"))

                                .route("orders-service", r -> r
                                                .path("/api/orders/**")
                                                .uri("lb://ORDER-SERVICE"))

                                .route("product-service", r -> r
                                                .path("/api/products", "/api/products/**")
                                                .uri("lb://PRODUCT-SERVICE"))

                                .route("media-service", r -> r
                                                .path("/api/media/**")
                                                .uri("lb://MEDIA-SERVICE"))

                                // Swagger/OpenAPI aggregation: proxy each service's /v3/api-docs
                                .route("openapi-user-service", r -> r
                                                .path("/v3/api-docs/user-service")
                                                .filters(f -> f.setPath("/v3/api-docs"))
                                                .uri("lb://USER-SERVICE"))

                                .route("openapi-product-service", r -> r
                                                .path("/v3/api-docs/product-service")
                                                .filters(f -> f.setPath("/v3/api-docs"))
                                                .uri("lb://PRODUCT-SERVICE"))

                                .route("openapi-media-service", r -> r
                                                .path("/v3/api-docs/media-service")
                                                .filters(f -> f.setPath("/v3/api-docs"))
                                                .uri("lb://MEDIA-SERVICE"))

                                .build();
        }
}
