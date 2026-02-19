package com.mypolicy.pipeline.matching.service;

import com.mypolicy.pipeline.matching.client.CustomerClient;
import com.mypolicy.pipeline.matching.client.PolicyClient;
import com.mypolicy.pipeline.matching.dto.CustomerDTO;
import com.mypolicy.pipeline.matching.dto.PolicyDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Matching Service: fuzzy matching, identity stitching, policy creation.
 * 
 * Consolidated Service: Part of data-pipeline-service.
 * OPTIMIZATION: Called directly by ProcessingService (no HTTP overhead).
 * 
 * External Dependencies: Still uses Feign clients for Customer (8081) and Policy (8085)
 * since those services remain separate.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

  private static final Logger log = LoggerFactory.getLogger(MatchingService.class);
  
  private final CustomerClient customerClient;  // External service - remains Feign
  private final PolicyClient policyClient;       // External service - remains Feign
  private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

  private static final int SIMILARITY_THRESHOLD = 3; // Max edit distance for fuzzy match

  /**
   * Process a standardized policy record from Processing Service.
   * 1. Try to match with existing customer (fuzzy logic).
   * 2. Create policy linked to customer.
   * 
   * Consolidation Benefit: Called directly by ProcessingService (same JVM).
   */
  public void processAndMatchPolicy(Map<String, Object> standardRecord) {
    log.info("[Matching] Processing policy record for identity stitching");
    
    // 1. Extract PII from the standardized Map
    String firstName = (String) standardRecord.get("firstName");
    String lastName = (String) standardRecord.get("lastName");
    String mobile = (String) standardRecord.get("mobileNumber");
    String policyNum = (String) standardRecord.get("policyNumber");
    String insurerId = (String) standardRecord.get("insurerId");
    String policyType = (String) standardRecord.get("policyType");

    log.debug("[Matching] Searching for customer: {} {}, mobile: {}", firstName, lastName, mobile);

    // 2. SEARCH: Look for existing customer by Mobile
    Optional<CustomerDTO> customerOpt = customerClient.searchByMobile(mobile);

    String resolvedCustomerId = null;

    if (customerOpt.isPresent()) {
      CustomerDTO masterRecord = customerOpt.get();
      String fullNameCsv = (firstName + " " + lastName).toLowerCase();
      String fullNameDb = (masterRecord.getFirstName() + " " + masterRecord.getLastName()).toLowerCase();

      // 3. VERIFY: Use Fuzzy Matching to confirm identity
      if (isSimilar(fullNameCsv, fullNameDb)) {
        resolvedCustomerId = masterRecord.getCustomerId();
        log.info("[Matching] Identity Stitched! Found match for {} -> {}", fullNameCsv, resolvedCustomerId);
      } else {
        log.warn("[Matching] Name mismatch: '{}' vs '{}' (distance > {})", 
            fullNameCsv, fullNameDb, SIMILARITY_THRESHOLD);
      }
    } else {
      log.warn("[Matching] No customer found with mobile: {}", mobile);
    }

    // 4. CREATE: If matched, stitch the policy to the Customer ID
    if (resolvedCustomerId != null) {
      PolicyDTO policyDto = new PolicyDTO();
      policyDto.setPolicyNumber(policyNum);
      policyDto.setCustomerId(resolvedCustomerId); // The "Stitch" happens here
      policyDto.setInsurerId(insurerId);
      policyDto.setPolicyType(policyType);
      policyDto.setPremiumAmount((BigDecimal) standardRecord.get("premiumAmount"));
      policyDto.setSumAssured((BigDecimal) standardRecord.get("sumAssured"));
      policyDto.setStatus("ACTIVE");

      try {
        policyClient.createPolicy(policyDto);
        log.info("[Matching] Policy {} successfully stitched to Customer {}", policyNum, resolvedCustomerId);
      } catch (Exception e) {
        log.error("[Matching] Failed to create policy {}", policyNum, e);
        throw new RuntimeException("Policy creation failed", e);
      }
    } else {
      log.warn("[Matching] No match found for policy {}. Routing to manual review.", policyNum);
      // TODO: Send to manual review queue
    }
  }

  /**
   * Calculate similarity between two strings using Levenshtein distance.
   * 
   * @return true if edit distance <= SIMILARITY_THRESHOLD (default 3)
   */
  public boolean isSimilar(String str1, String str2) {
    if (str1 == null || str2 == null)
      return false;
    
    int distance = levenshteinDistance.apply(str1.toLowerCase(), str2.toLowerCase());
    log.debug("[Matching] Fuzzy match: '{}' vs '{}' -> distance={}", str1, str2, distance);
    
    return distance <= SIMILARITY_THRESHOLD;
  }
}
