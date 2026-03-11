package com.mypolicy.ingestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiKeyFilterConfig {

  @Value("${ingestion.customer.api-keys:}")
  private String apiKeys;

  @Bean
  public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter() {
    FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new ApiKeyAuthFilter(apiKeys));
    registration.addUrlPatterns("/api/public/v1/ingestion/*");
    registration.setOrder(1);
    return registration;
  }
}
