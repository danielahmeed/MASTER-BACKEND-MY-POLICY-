package com.mypolicy.customer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

  @GetMapping({ "/", "/health", "/api/health" })
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("service", "Customer Service");
    response.put("version", "0.0.1-SNAPSHOT");
    response.put("timestamp", LocalDateTime.now());
    response.put("port", "8082");
    response.put("database", "H2 In-Memory");

    Map<String, String> endpoints = new HashMap<>();
    endpoints.put("register", "POST /api/v1/customers/register");
    endpoints.put("login", "POST /api/v1/customers/login");
    endpoints.put("h2-console", "GET /h2-console");

    response.put("availableEndpoints", endpoints);

    return ResponseEntity.ok(response);
  }
}
