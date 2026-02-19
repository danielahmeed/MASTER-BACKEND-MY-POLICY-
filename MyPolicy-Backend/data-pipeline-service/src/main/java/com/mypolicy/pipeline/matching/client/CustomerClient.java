package com.mypolicy.pipeline.matching.client;

import com.mypolicy.pipeline.matching.dto.CustomerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;

/**
 * Feign client for Customer Service (external, port 8081).
 * 
 * IMPORTANT: Customer Service remains separate - this is a genuine external call.
 */
@FeignClient(name = "customer-service", url = "${customer.service.url:http://localhost:8081}")
public interface CustomerClient {

  @GetMapping("/api/v1/customers/{customerId}")
  CustomerDTO getCustomerById(@PathVariable("customerId") String customerId);

  @GetMapping("/api/v1/customers/search/mobile/{mobile}")
  Optional<CustomerDTO> searchByMobile(@PathVariable("mobile") String mobile);
}
