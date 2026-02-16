package com.mypolicy.policy.service;

import com.mypolicy.policy.dto.PolicyRequest;
import com.mypolicy.policy.model.Policy;
import com.mypolicy.policy.model.PolicyStatus;

import java.util.List;

public interface PolicyService {
  Policy createPolicy(PolicyRequest request);

  List<Policy> getPoliciesByCustomerId(String customerId);

  Policy getPolicyById(String id);
}
