package com.mypolicy.policy.controller;

import com.mypolicy.policy.dto.PolicyCorrectionRequest;
import com.mypolicy.policy.dto.PolicyRequest;
import com.mypolicy.policy.model.Policy;
import com.mypolicy.policy.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

  private final PolicyService policyService;

  @PostMapping
  public ResponseEntity<Policy> createPolicy(@RequestBody PolicyRequest request) {
    return ResponseEntity.ok(policyService.createPolicy(request));
  }

  @GetMapping("/customer/{customerId}")
  public ResponseEntity<List<Policy>> getPoliciesByCustomer(@PathVariable String customerId) {
    return ResponseEntity.ok(policyService.getPoliciesByCustomerId(customerId));
  }

  @GetMapping("/search")
  public ResponseEntity<Policy> getPolicyByNumberAndInsurer(
      @RequestParam String policyNumber,
      @RequestParam String insurerId) {
    return ResponseEntity.ok(policyService.getPolicyByNumberAndInsurerId(policyNumber, insurerId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Policy> getPolicyById(@PathVariable String id) {
    return ResponseEntity.ok(policyService.getPolicyById(id));
  }

  /**
   * Correction/Patch API – update policy record (support/admin).
   * Only non-null fields are updated. Reason is mandatory for audit.
   */
  @PatchMapping("/{id}")
  public ResponseEntity<Policy> correctPolicy(
      @PathVariable String id,
      @Valid @RequestBody PolicyCorrectionRequest request) {
    return ResponseEntity.ok(policyService.correctPolicy(id, request));
  }
}
