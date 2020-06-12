package com.sfg.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class GatewayRoutesLocalDiscoveryConfig {

    @Profile({"!local-discovery & !digitalocean"})
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder){

        return builder.routes()
                .route(r -> r.path("/api/v1/beer/*", "/api/v1/beerUpc/*")
                        .uri("http://localhost:8080")
                        .id("beer-service"))
                .route(r -> r.path("/api/v1/beer/*/inventory" )
                        .uri("http://localhost:8082")
                        .id("inventory-service"))
                .route(r -> r.path(("/api/v1/customers/**"))
                        .uri("http://localhost:8081")
                        .id("order-service"))
                .build();
    }

    @Profile({"local-discovery", "digitalocean"})
    @Bean
    public RouteLocator gatewayRoutesLocalDiscovery(RouteLocatorBuilder builder){
        return builder.routes()
                .route(r -> r.path("/api/v1/beer*", "/api/v1/beer/*", "/api/v1/beerUpc/*")
                        .uri("lb://beer-service")
                        .id("beer-service"))
                .route(r -> r.path("/api/v1/beer/*/inventory")
                        .filters(f -> f.circuitBreaker(c -> c.setName("inventoryCB")
                                .setFallbackUri("forward:/inventory-failover")
                                .setRouteId("inventory-failover-route")))
                        .uri("lb://inventory-service")
                        .id("inventory-service"))
                .route(r -> r.path("/inventory-failover/**")
                        //  .filters(f -> f.rewritePath("/inventory-failover(?<segment>/?.*)", "/${segment}"))
                        .uri("lb://inventory-failover")
                        .id("inventory-failover-service"))
                .route(r -> r.path("/api/v1/customers/**")
                        .uri("lb://order-service")
                        .id("order-service"))
                .build();
    }
}
