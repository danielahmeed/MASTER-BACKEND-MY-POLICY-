package com.mypolicy.bff.client;

import com.mypolicy.bff.dto.PolicyDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "policy-service", url = "${policy.service.url}")
public interface PolicyClient {

  @GetMapping("/api/v1/policies/customer/{customerId}")
  List<PolicyDTO> getPoliciesByCustomer(@PathVariable("customerId") String customerId);

  @GetMapping("/api/v1/policies/{id}")
  PolicyDTO getPolicyById(@PathVariable("id") String id);
}
