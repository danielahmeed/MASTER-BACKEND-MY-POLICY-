package com.mypolicy.ingestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS for the exposed customer ingestion API.
 */
@Configuration
public class PublicApiCorsConfig {

  @Value("${ingestion.customer.cors.allowed-origins:*}")
  private String allowedOrigins;

  @Bean
  public CorsConfigurationSource publicApiCorsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(parseOrigins(allowedOrigins));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("Authorization", "X-API-Key", "Content-Type"));
    config.setExposedHeaders(List.of("Location"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/public/**", config);
    return source;
  }

  private List<String> parseOrigins(String value) {
    if (value == null || "*".equals(value.trim())) {
      return List.of("*");
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }
}
