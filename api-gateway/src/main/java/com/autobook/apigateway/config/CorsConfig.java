package com.autobook.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");

        // Use the reactive UrlBasedCorsConfigurationSource
        CorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        ((UrlBasedCorsConfigurationSource) source).registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(source);
    }
}
