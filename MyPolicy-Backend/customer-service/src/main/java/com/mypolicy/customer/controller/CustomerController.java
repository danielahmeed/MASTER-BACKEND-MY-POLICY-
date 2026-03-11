package com.mypolicy.customer.controller;

import com.mypolicy.customer.dto.AuthResponse;
import com.mypolicy.customer.dto.CustomerBulkCreateRequest;
import com.mypolicy.customer.dto.CustomerBulkCreateResponse;
import com.mypolicy.customer.dto.CustomerRegistrationRequest;
import com.mypolicy.customer.dto.CustomerResponse;
import com.mypolicy.customer.dto.CustomerUpdateRequest;
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

  @PostMapping("/bulk")
  public ResponseEntity<CustomerBulkCreateResponse> bulkCreate(@Valid @RequestBody java.util.List<CustomerBulkCreateRequest> requests) {
    return ResponseEntity.status(HttpStatus.CREATED).body(customerService.bulkCreateCustomers(requests));
  }

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

  @GetMapping("/search/mobile/{mobile}")
  public ResponseEntity<CustomerResponse> searchByMobile(@PathVariable String mobile) {
    return customerService.findByMobileNumber(mobile)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/search/email/{email}")
  public ResponseEntity<CustomerResponse> searchByEmail(@PathVariable String email) {
    return customerService.findByEmail(email)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/search/pan/{pan}")
  public ResponseEntity<CustomerResponse> searchByPan(@PathVariable String pan) {
    return customerService.findByPanNumber(pan)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{customerId}")
  public ResponseEntity<CustomerResponse> updateCustomer(
      @PathVariable String customerId,
      @Valid @RequestBody CustomerUpdateRequest request) {
    return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
  }
}
