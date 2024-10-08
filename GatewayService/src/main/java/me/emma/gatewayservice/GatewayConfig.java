package me.emma.gatewayservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("http://localhost:4200"); // Allow specific origin
        corsConfig.addAllowedHeader("*"); // Allow any header
        corsConfig.addAllowedMethod("*"); // Allow any method
        corsConfig.setAllowCredentials(true); // Allow credentials

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    Logger logger = LoggerFactory.getLogger(GatewayConfig.class);
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("admin", r -> r.path("/api/admin")
                        .filters(f -> f.rewritePath("/api/admin", "/admin")
                                .circuitBreaker(config -> config.setName("productsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback")))
                        .uri("lb://admin-service"))

                .route("admin", r -> r.path("/api/admin/**")
                        .filters(f->f.rewritePath("/api/(?<service>.*)/(?<remaining>.*)",
                                        "/${service}/${remaining}")
                                .circuitBreaker(config -> config.setName("adminCircuitBreaker")
                                        .setFallbackUri("forward:/fallback"))
                        )
                        .uri("lb://admin-service"))

                .route("orders", r -> r.path("/api/orders")
                        .filters(f -> f.rewritePath("/api/orders", "/orders")
                                .circuitBreaker(config -> config.setName("productsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback")))
                        .uri("lb://order-service"))

                .route("orders", r -> r.path("/api/orders/**")
                        .filters(f->f.rewritePath("/api/(?<service>.*)/(?<remaining>.*)",
                                        "/${service}/${remaining}")
                                .circuitBreaker(config -> config.setName("orderCircuitBreaker")
                                        .setFallbackUri("forward:/fallback"))
                        )
                        .uri("lb://order-service"))

                .route("products", r -> r.path("/api/products")
                        .filters(f -> f.rewritePath("/api/products", "/products")
                                .circuitBreaker(config -> config.setName("productsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback")))
                        .uri("lb://product-service"))

                .route("products", r -> r.path("/api/products/**")
                        .filters(f->f.rewritePath("/api/(?<service>.*)/(?<remaining>.*)",
                                        "/${service}/${remaining}")
                                .circuitBreaker(config -> config.setName("productsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback"))
                        )
                        .uri("lb://product-service"))

                .route("images", r -> r.path("/api/images")
                        .filters(f -> f.rewritePath("/api/images", "/images")
                                .circuitBreaker(config -> config.setName("productsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback")))
                        .uri("lb://image-service"))

                .route("images", r -> r.path("/api/images/**")
                        .filters(f->f.rewritePath("/api/(?<service>.*)/(?<remaining>.*)",
                                        "/${service}/${remaining}")
                                .circuitBreaker(config -> config.setName("productsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback"))
                        )
                        .uri("lb://image-service"))


                .build();
    }

    @Bean
    public GlobalFilter customGlobalFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestMethod = request.getMethod().name();
            String requestPath = request.getPath().toString();
            logger.info("Incoming request " + requestMethod + " " + requestPath);
            long startTime = System.currentTimeMillis();

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // Capture and log response details
                ServerHttpResponse response = exchange.getResponse();
                HttpStatusCode responseStatus = response.getStatusCode();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Outgoing response with status code " + responseStatus + " processed in " + duration + " ms");
            }));
        };
    }
}
