package com.mypolicy.pipeline.matching.service;

import com.mypolicy.pipeline.matching.client.PolicyClient;
import com.mypolicy.pipeline.matching.dto.PolicyDTO;
import com.mypolicy.pipeline.portfolio.service.CustomerPortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Matching Service: uses generic rule layer (mobile, email, PAN, DOB) to resolve
 * customer, then stitches policy to Customer Master.
 */
@Service
public class MatchingService {

  private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

  private final CustomerMatchingRuleService matchingRuleService;
  private final PolicyClient policyClient;
  private final CustomerPortfolioService portfolioService;

  public MatchingService(CustomerMatchingRuleService matchingRuleService, PolicyClient policyClient,
      CustomerPortfolioService portfolioService) {
    this.matchingRuleService = matchingRuleService;
    this.policyClient = policyClient;
    this.portfolioService = portfolioService;
  }

  /**
   * Process a standardized policy record: resolve customer via generic matching rules,
   * then create policy linked to that customer.
   * @return Optional with failure reason if skipped; empty if policy was created
   */
  public Optional<String> processAndMatchPolicy(Map<String, Object> standardRecord) {
    String policyNum = (String) standardRecord.get("policyNumber");
    log.debug("[Matching] Resolving customer for policy {}", policyNum);

    var matchResult = matchingRuleService.resolveCustomer(standardRecord);

    if (!matchResult.isSuccess()) {
      log.warn("[Matching] No match for policy {}: {}", policyNum, matchResult.failureReason());
      return Optional.of(matchResult.failureReason());
    }

    String resolvedCustomerId = matchResult.customerId();
    log.info("[Matching] Identity stitched: policy {} -> customer {}", policyNum, resolvedCustomerId);

    PolicyDTO policyDto = new PolicyDTO();
    policyDto.setPolicyNumber(policyNum);
    policyDto.setCustomerId(resolvedCustomerId);
    policyDto.setInsurerId((String) standardRecord.get("insurerId"));
    policyDto.setPolicyType((String) standardRecord.get("policyType"));
    policyDto.setPremiumAmount(toBigDecimal(standardRecord.get("premiumAmount")));
    policyDto.setSumAssured(toBigDecimal(standardRecord.get("sumAssured")));
    policyDto.setPlanName((String) standardRecord.get("planName"));
    policyDto.setStartDate(toLocalDate(standardRecord.get("policyStartDate")));
    policyDto.setEndDate(toLocalDate(standardRecord.get("policyEndDate")));
    policyDto.setStatus("ACTIVE");

    try {
      PolicyDTO created = policyClient.createPolicy(policyDto);
      // Update consolidated portfolio (customer + all policies)
      String startDate = standardRecord.get("policyStartDate") != null ? standardRecord.get("policyStartDate").toString() : null;
      String endDate = standardRecord.get("policyEndDate") != null ? standardRecord.get("policyEndDate").toString() : null;
      portfolioService.addPolicyToCustomer(resolvedCustomerId, created.getId(), policyNum,
          (String) standardRecord.get("insurerId"), (String) standardRecord.get("policyType"),
          (String) standardRecord.get("planName"), policyDto.getPremiumAmount(), policyDto.getSumAssured(),
          startDate, endDate, policyDto.getStatus());
      log.info("[Matching] Policy {} created and linked to customer {}", policyNum, resolvedCustomerId);
      return Optional.empty();
    } catch (Exception e) {
      log.error("[Matching] Failed to create policy {}", policyNum, e);
      return Optional.of("Policy creation failed: " + e.getMessage());
    }
  }

  private static LocalDate toLocalDate(Object value) {
    if (value == null) return null;
    if (value instanceof LocalDate) return (LocalDate) value;
    try {
      return LocalDate.parse(value.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private static BigDecimal toBigDecimal(Object value) {
    if (value == null) return BigDecimal.ZERO;
    if (value instanceof BigDecimal) return (BigDecimal) value;
    try {
      return new BigDecimal(value.toString().replaceAll("[^0-9.\\-]", ""));
    } catch (NumberFormatException e) {
      return BigDecimal.ZERO;
    }
  }
}
