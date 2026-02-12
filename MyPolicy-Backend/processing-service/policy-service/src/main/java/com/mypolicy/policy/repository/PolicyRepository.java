package com.mypolicy.policy.repository;

import com.mypolicy.policy.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, String> {
  List<Policy> findByCustomerId(String customerId);

  Optional<Policy> findByPolicyNumberAndInsurerId(String policyNumber, String insurerId);
}
