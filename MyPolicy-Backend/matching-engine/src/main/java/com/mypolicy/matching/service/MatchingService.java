package com.mypolicy.matching.service;

import com.mypolicy.matching.client.CustomerClient;
import com.mypolicy.matching.client.PolicyClient;
import com.mypolicy.matching.dto.CustomerDTO;
import com.mypolicy.matching.dto.PolicyDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

  private final CustomerClient customerClient;
  private final PolicyClient policyClient;
  private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

  private static final int SIMILARITY_THRESHOLD = 3; // Max edit distance for fuzzy match

  /**
   * Process a standardized policy record from Processing Service
   * 1. Try to match with existing customer
   * 2. Create policy linked to customer
   */
  public void processAndMatchPolicy(Map<String, Object> standardRecord) {
    log.info("Processing policy record: {}", standardRecord);

    // Extract customer identifiers
    String mobileNumber = (String) standardRecord.get("mobileNumber");
    String email = (String) standardRecord.get("email");
    String panNumber = (String) standardRecord.get("panNumber");
    String firstName = (String) standardRecord.get("firstName");
    String lastName = (String) standardRecord.get("lastName");

    // TODO: In production, query Customer Service with search criteria
    // For now, assume we have a customerId (from manual registration or fuzzy
    // match)
    String customerId = findOrCreateCustomer(firstName, lastName, email, mobileNumber, panNumber);

    // Create Policy DTO
    PolicyDTO policyDTO = new PolicyDTO();
    policyDTO.setCustomerId(customerId);
    policyDTO.setInsurerId((String) standardRecord.get("insurerId"));
    policyDTO.setPolicyNumber((String) standardRecord.get("policyNumber"));
    policyDTO.setPolicyType((String) standardRecord.get("policyType"));
    policyDTO.setPlanName((String) standardRecord.get("planName"));
    policyDTO.setPremiumAmount((java.math.BigDecimal) standardRecord.get("premiumAmount"));
    policyDTO.setSumAssured((java.math.BigDecimal) standardRecord.get("sumAssured"));
    policyDTO.setStartDate((java.time.LocalDate) standardRecord.get("startDate"));
    policyDTO.setEndDate((java.time.LocalDate) standardRecord.get("endDate"));
    policyDTO.setStatus((String) standardRecord.getOrDefault("status", "ACTIVE"));

    // Save Policy
    PolicyDTO createdPolicy = policyClient.createPolicy(policyDTO);
    log.info("Policy created successfully: {}", createdPolicy);
  }

  /**
   * Fuzzy matching logic to find existing customer
   * In production, this would query Customer Service with search API
   */
  private String findOrCreateCustomer(String firstName, String lastName, String email,
      String mobileNumber, String panNumber) {
    // TODO: Implement actual fuzzy matching with Customer Service
    // For now, return a placeholder customerId
    // In real implementation:
    // 1. Query Customer Service with search criteria
    // 2. Use Levenshtein distance for name matching
    // 3. Exact match on PAN/Email/Mobile
    // 4. If no match, create new customer record

    log.info("Searching for customer: {} {}, email: {}, mobile: {}",
        firstName, lastName, email, mobileNumber);

    // Placeholder - assume customer exists
    return "CUST_PLACEHOLDER_001";
  }

  /**
   * Calculate similarity between two strings using Levenshtein distance
   */
  public boolean isSimilar(String str1, String str2) {
    if (str1 == null || str2 == null)
      return false;
    int distance = levenshteinDistance.apply(str1.toLowerCase(), str2.toLowerCase());
    return distance <= SIMILARITY_THRESHOLD;
  }
}
