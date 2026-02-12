package com.mypolicy.matching.client;

import com.mypolicy.matching.dto.CustomerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "http://localhost:8081")
public interface CustomerClient {

  @GetMapping("/api/v1/customers/{customerId}")
  CustomerDTO getCustomerById(@PathVariable("customerId") String customerId);
}
