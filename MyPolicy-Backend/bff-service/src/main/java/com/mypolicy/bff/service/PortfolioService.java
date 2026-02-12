package com.mypolicy.bff.service;

import com.mypolicy.bff.client.CustomerClient;
import com.mypolicy.bff.client.PolicyClient;
import com.mypolicy.bff.dto.CustomerDTO;
import com.mypolicy.bff.dto.PolicyDTO;
import com.mypolicy.bff.dto.PortfolioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

  private final CustomerClient customerClient;
  private final PolicyClient policyClient;

  /**
   * Aggregates customer and policy data into unified portfolio view
   */
  public PortfolioResponse getPortfolio(String customerId) {
    log.info("Fetching portfolio for customer: {}", customerId);

    // Call Customer Service
    CustomerDTO customer = customerClient.getCustomerById(customerId);

    // Call Policy Service
    List<PolicyDTO> policies = policyClient.getPoliciesByCustomer(customerId);

    // Calculate aggregates
    BigDecimal totalPremium = policies.stream()
        .map(PolicyDTO::getPremiumAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalCoverage = policies.stream()
        .map(PolicyDTO::getSumAssured)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Build response
    PortfolioResponse response = new PortfolioResponse();
    response.setCustomer(customer);
    response.setPolicies(policies);
    response.setTotalPolicies(policies.size());
    response.setTotalPremium(totalPremium);
    response.setTotalCoverage(totalCoverage);

    log.info("Portfolio fetched: {} policies, total premium: {}", policies.size(), totalPremium);
    return response;
  }
}
