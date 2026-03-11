package com.mypolicy.policy.controller;

import com.mypolicy.policy.dto.PolicyRequest;
import com.mypolicy.policy.model.Policy;
import com.mypolicy.policy.model.PolicyStatus;
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
  public ResponseEntity<Policy> createPolicy(@Valid @RequestBody PolicyRequest request) {
    return ResponseEntity.ok(policyService.createPolicy(request));
  }

  @GetMapping("/customer/{customerId}")
  public ResponseEntity<List<Policy>> getPoliciesByCustomer(@PathVariable String customerId) {
    return ResponseEntity.ok(policyService.getPoliciesByCustomerId(customerId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Policy> getPolicyById(@PathVariable String id) {
    return ResponseEntity.ok(policyService.getPolicyById(id));
  }

  @GetMapping
  public ResponseEntity<List<Policy>> getAllPolicies() {
    return ResponseEntity.ok(policyService.getAllPolicies());
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<Policy> updatePolicyStatus(
      @PathVariable String id,
      @RequestParam PolicyStatus status) {
    return ResponseEntity.ok(policyService.updatePolicyStatus(id, status));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePolicy(@PathVariable String id) {
    policyService.deletePolicy(id);
    return ResponseEntity.noContent().build();
  }
}
