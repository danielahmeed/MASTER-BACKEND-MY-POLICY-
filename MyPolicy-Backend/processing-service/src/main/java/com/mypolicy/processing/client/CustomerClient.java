package com.mypolicy.processing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "customer-service", url = "${customer.service.url}")
public interface CustomerClient {

  @GetMapping("/api/v1/customers/{customerId}")
  Map<String, Object> getCustomerById(@PathVariable("customerId") String customerId);

  @GetMapping("/api/v1/customers/by-pan/{panNumber}")
  Map<String, Object> getCustomerByPan(@PathVariable("panNumber") String panNumber);

  @PatchMapping("/api/v1/customers/{customerId}")
  Map<String, Object> correctCustomer(
      @PathVariable("customerId") String customerId,
      @RequestBody Map<String, Object> request);
}
