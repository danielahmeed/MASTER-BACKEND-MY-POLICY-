package com.mypolicy.ingestion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validates X-API-Key header for the public customer ingestion API.
 * Used for /api/public/* endpoints.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

  public static final String API_KEY_HEADER = "X-API-Key";

  private final Set<String> validApiKeys;

  public ApiKeyAuthFilter(String apiKeysConfig) {
    if (apiKeysConfig == null || apiKeysConfig.isBlank()) {
      this.validApiKeys = Set.of();
    } else {
      this.validApiKeys = Stream.of(apiKeysConfig.split(","))
          .map(String::trim)
          .filter(s -> !s.isBlank())
          .collect(Collectors.toSet());
    }
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    String apiKey = request.getHeader(API_KEY_HEADER);

    if (apiKey == null || apiKey.isBlank()) {
      sendError(response, 401, "Missing X-API-Key header");
      return;
    }

    if (!validApiKeys.contains(apiKey)) {
      sendError(response, 401, "Invalid API key");
      return;
    }

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/public/");
  }

  private void sendError(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }
}
