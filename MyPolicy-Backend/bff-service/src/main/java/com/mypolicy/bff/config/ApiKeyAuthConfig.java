package com.mypolicy.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiKeyAuthConfig {

  @Value("${ingestion.customer.api-keys:}")
  private String apiKeys;

  @Bean
  public ApiKeyAuthFilter apiKeyAuthFilter() {
    return new ApiKeyAuthFilter(apiKeys);
  }
}
