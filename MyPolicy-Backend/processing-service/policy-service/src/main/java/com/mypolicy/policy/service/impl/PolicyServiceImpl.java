package com.mypolicy.policy.service.impl;

import com.mypolicy.policy.dto.PolicyRequest;
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
        .orElseThrow(() -> new RuntimeException("Policy not found"));
  }
}
