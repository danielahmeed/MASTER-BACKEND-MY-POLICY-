package com.mypolicy.matching.service;

import com.mypolicy.matching.client.CustomerClient;
import com.mypolicy.matching.client.PolicyClient;
import com.mypolicy.matching.dto.CustomerDTO;
import com.mypolicy.matching.dto.PolicyDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

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
    // 1. Extract PII from the standardized Map
    String firstName = (String) standardRecord.get("firstName");
    String lastName = (String) standardRecord.get("lastName");
    String mobile = (String) standardRecord.get("mobileNumber");
    String policyNum = (String) standardRecord.get("policyNumber");

    // 2. SEARCH: Look for existing customer by Mobile
    // Note: You'll need to update CustomerClient to include searchByMobile
    Optional<CustomerDTO> customerOpt = customerClient.searchByMobile(mobile);

    String resolvedCustomerId = null;

    if (customerOpt.isPresent()) {
      CustomerDTO masterRecord = customerOpt.get();
      String fullNameCsv = (firstName + " " + lastName).toLowerCase();
      String fullNameDb = (masterRecord.getFirstName() + " " + masterRecord.getLastName()).toLowerCase();

      // 3. VERIFY: Use Fuzzy Matching to confirm identity
      if (isSimilar(fullNameCsv, fullNameDb)) {
        resolvedCustomerId = masterRecord.getCustomerId();
        log.info("Identity Stitched! Found match for {} -> {}", fullNameCsv, resolvedCustomerId);
      }
    }

    // 4. CREATE: If matched, stitch the policy to the Customer ID
    if (resolvedCustomerId != null) {
      PolicyDTO policyDto = new PolicyDTO();
      policyDto.setPolicyNumber(policyNum);
      policyDto.setCustomerId(resolvedCustomerId); // The "Stitch" happens here
      policyDto.setPremiumAmount((BigDecimal) standardRecord.get("premiumAmount"));

      policyClient.createPolicy(policyDto);
      log.info("Policy {} successfully stitched to Customer {}", policyNum, resolvedCustomerId);
    } else {
      log.warn("No match found for policy {}. Routing to manual review.", policyNum);
    }
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
