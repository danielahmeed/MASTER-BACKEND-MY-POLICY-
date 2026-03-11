package com.mypolicy.policy.service.impl;

import com.mypolicy.policy.dto.PolicyRequest;
import com.mypolicy.policy.exception.DuplicatePolicyException;
import com.mypolicy.policy.exception.PolicyNotFoundException;
import com.mypolicy.policy.model.Policy;
import com.mypolicy.policy.model.PolicyStatus;
import com.mypolicy.policy.repository.PolicyRepository;
import com.mypolicy.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

  private final PolicyRepository repository;

  @Override
  public Policy createPolicy(PolicyRequest request) {
    // Check for duplicate policy
    repository.findByPolicyNumberAndInsurerId(request.getPolicyNumber(), request.getInsurerId())
        .ifPresent(existing -> {
          throw new DuplicatePolicyException(request.getPolicyNumber(), request.getInsurerId());
        });

    Policy policy = Policy.builder()
        .customerId(request.getCustomerId())
        .insurerId(request.getInsurerId())
        .policyNumber(request.getPolicyNumber())
        .policyType(request.getPolicyType())
        .planName(request.getPlanName())
        .premiumAmount(request.getPremiumAmount())
        .sumAssured(request.getSumAssured())
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .status(PolicyStatus.valueOf(request.getStatus()))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    return repository.save(policy);
  }

  @Override
  public List<Policy> getPoliciesByCustomerId(String customerId) {
    return repository.findByCustomerId(customerId);
  }

  @Override
  public Policy getPolicyById(String id) {
    return repository.findById(id)
        .orElseThrow(() -> new PolicyNotFoundException(id, "id"));
  }

  @Override
  public Optional<Policy> findByPolicyNumberAndInsurerId(String policyNumber, String insurerId) {
    return repository.findByPolicyNumberAndInsurerId(policyNumber, insurerId);
  }

  @Override
  public List<Policy> getAllPolicies() {
    return repository.findAll();
  }

  @Override
  public Policy updatePolicyStatus(String id, PolicyStatus status) {
    Policy policy = repository.findById(id)
        .orElseThrow(() -> new PolicyNotFoundException(id, "id"));
    
    policy.setStatus(status);
    policy.setUpdatedAt(LocalDateTime.now());
    
    return repository.save(policy);
  }

  @Override
  public void deletePolicy(String id) {
    if (!repository.existsById(id)) {
      throw new PolicyNotFoundException(id, "id");
    }
    repository.deleteById(id);
  }
}
