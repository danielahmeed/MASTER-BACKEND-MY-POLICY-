package com.mypolicy.bff.client;

import com.mypolicy.bff.dto.AuthResponse;
import com.mypolicy.bff.dto.CustomerDTO;
import com.mypolicy.bff.dto.LoginRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "customer-service", url = "${customer.service.url}")
public interface CustomerClient {

  @PostMapping("/api/v1/customers/register")
  CustomerDTO register(@RequestBody Object request);

  @PostMapping("/api/v1/customers/login")
  AuthResponse login(@RequestBody LoginRequest request);

  @GetMapping("/api/v1/customers/{customerId}")
  CustomerDTO getCustomerById(@PathVariable("customerId") String customerId);

  @PutMapping("/api/v1/customers/{customerId}")
  CustomerDTO updateCustomer(@PathVariable("customerId") String customerId, @RequestBody Object request);
}
