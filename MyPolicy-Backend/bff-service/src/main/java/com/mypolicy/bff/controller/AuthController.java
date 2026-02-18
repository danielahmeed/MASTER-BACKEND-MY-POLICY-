package com.mypolicy.bff.controller;

import com.mypolicy.bff.client.CustomerClient;
import com.mypolicy.bff.dto.AuthResponse;
import com.mypolicy.bff.dto.CustomerDTO;
import com.mypolicy.bff.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/auth")
@RequiredArgsConstructor
public class AuthController {

  private final CustomerClient customerClient;

  @PostMapping("/register")
  public ResponseEntity<CustomerDTO> register(@RequestBody Object request) {
    return ResponseEntity.ok(customerClient.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(customerClient.login(request));
  }

  @PutMapping("/customer/{customerId}")
  public ResponseEntity<CustomerDTO> updateCustomer(
      @PathVariable String customerId,
      @RequestBody Object request) {
    return ResponseEntity.ok(customerClient.updateCustomer(customerId, request));
  }
}
