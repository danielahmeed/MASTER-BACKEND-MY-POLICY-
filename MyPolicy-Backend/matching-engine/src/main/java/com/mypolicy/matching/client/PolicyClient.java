package com.mypolicy.matching.client;

import com.mypolicy.matching.dto.PolicyDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "policy-service", url = "http://localhost:8085")
public interface PolicyClient {

  @PostMapping("/api/v1/policies")
  PolicyDTO createPolicy(@RequestBody PolicyDTO policyDTO);
}
