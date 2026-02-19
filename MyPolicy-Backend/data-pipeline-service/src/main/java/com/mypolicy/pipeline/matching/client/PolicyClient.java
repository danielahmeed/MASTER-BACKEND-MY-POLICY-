package com.mypolicy.pipeline.matching.client;

import com.mypolicy.pipeline.matching.dto.PolicyDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Policy Service (external, port 8085).
 * 
 * IMPORTANT: Policy Service remains separate - this is a genuine external call.
 */
@FeignClient(name = "policy-service", url = "${policy.service.url:http://localhost:8085}")
public interface PolicyClient {

  @PostMapping("/api/v1/policies")
  PolicyDTO createPolicy(@RequestBody PolicyDTO policyDTO);
}
