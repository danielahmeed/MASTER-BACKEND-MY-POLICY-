package com.mypolicy.processing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "policy-service", url = "${policy.service.url}")
public interface PolicyClient {

  @GetMapping("/api/v1/policies/search")
  Map<String, Object> getPolicyByNumberAndInsurer(
      @RequestParam("policyNumber") String policyNumber,
      @RequestParam("insurerId") String insurerId);

  @GetMapping("/api/v1/policies/{id}")
  Map<String, Object> getPolicyById(@PathVariable("id") String id);

  @PatchMapping("/api/v1/policies/{id}")
  Map<String, Object> correctPolicy(
      @PathVariable("id") String id,
      @RequestBody Map<String, Object> request);
}
