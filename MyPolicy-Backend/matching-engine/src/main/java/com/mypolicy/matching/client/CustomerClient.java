package com.mypolicy.matching.client;

import com.mypolicy.matching.dto.CustomerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;

@FeignClient(name = "customer-service", url = "${customer.service.url:http://localhost:8081}")
public interface CustomerClient {

  // Method 1: Fetch by ID
  @GetMapping("/api/v1/customers/{customerId}")
  CustomerDTO getCustomerById(@PathVariable("customerId") String customerId);

  // Method 2: Search by Mobile (The one we just added for Stitching)
  @GetMapping("/api/v1/customers/search/mobile/{mobile}")
  Optional<CustomerDTO> searchByMobile(@PathVariable("mobile") String mobile);
}