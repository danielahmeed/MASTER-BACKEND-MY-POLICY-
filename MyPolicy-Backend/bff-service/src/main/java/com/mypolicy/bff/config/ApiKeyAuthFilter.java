package com.mypolicy.bff.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validates X-API-Key header for the public customer ingestion API.
 * Customers receive an API key to authenticate their upload requests.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

  public static final String API_KEY_HEADER = "X-API-Key";
  private static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

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
      sendError(response, HttpStatus.UNAUTHORIZED, "Missing X-API-Key header");
      return;
    }

    if (!validApiKeys.contains(apiKey)) {
      sendError(response, HttpStatus.UNAUTHORIZED, "Invalid API key");
      return;
    }

    var auth = new UsernamePasswordAuthenticationToken(
        "customer-api",
        null,
        List.of(new SimpleGrantedAuthority(ROLE_CUSTOMER)));
    SecurityContextHolder.getContext().setAuthentication(auth);

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/public/");
  }

  private void sendError(HttpServletResponse response, HttpStatus status, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }
}
