package com.mypolicy.customer.controller;

import com.mypolicy.customer.dto.AuthResponse;
import com.mypolicy.customer.dto.CustomerRegistrationRequest;
import com.mypolicy.customer.dto.CustomerResponse;
import com.mypolicy.customer.dto.LoginRequest;
import com.mypolicy.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping("/register")
  public ResponseEntity<CustomerResponse> register(@Valid @RequestBody CustomerRegistrationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(customerService.registerCustomer(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(customerService.login(request));
  }

  @GetMapping("/{customerId}")
  public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String customerId) {
    return ResponseEntity.ok(customerService.getCustomerById(customerId));
  }
}
