package com.mypolicy.pipeline.matching.service;

import com.mypolicy.pipeline.matching.client.CustomerClient;
import com.mypolicy.pipeline.matching.dto.CustomerDTO;
import com.mypolicy.pipeline.matching.dto.MatchResult;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Generic rule layer to map policy records to Customer Master using
 * mobile, email, PAN, and DOB (and combinations) without hard-coding.
 * Priority: mobile → PAN → email → composite (e.g. mobile+DOB, email+DOB).
 */
@Service
@RequiredArgsConstructor
public class CustomerMatchingRuleService {

  private static final Logger log = LoggerFactory.getLogger(CustomerMatchingRuleService.class);
  private static final int NAME_SIMILARITY_THRESHOLD = 3;

  private final CustomerClient customerClient;

  public CustomerMatchingRuleService(CustomerClient customerClient) {
    this.customerClient = customerClient;
  }
  private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

  /**
   * Resolve customer ID from standardized policy record using generic matching rules.
   * Tries in order: mobile, PAN, email; then verifies with DOB/name when available.
   * @return MatchResult with customerId (success) or failureReason (when no match or verification fails)
   */
  public MatchResult resolveCustomer(Map<String, Object> standardRecord) {
    String mobile = normalize((String) standardRecord.get("mobileNumber"));
    String email = normalize((String) standardRecord.get("email"));
    String pan = normalize((String) standardRecord.get("panNumber"));
    String dob = (String) standardRecord.get("dateOfBirth");
    String customerName = (String) standardRecord.get("customerName");

    // 1. Strong identifiers first (single-field match)
    Optional<CustomerDTO> byMobile = mobile != null && !mobile.isEmpty() ? searchByMobile(mobile) : Optional.empty();
    if (byMobile.isPresent() && verifyMatch(byMobile.get(), customerName, dob)) {
      log.debug("[MatchingRule] Matched by mobile");
      return MatchResult.success(byMobile.get().getCustomerId());
    }

    Optional<CustomerDTO> byPan = pan != null && !pan.isEmpty() ? searchByPan(pan) : Optional.empty();
    if (byPan.isPresent() && verifyMatch(byPan.get(), customerName, dob)) {
      log.debug("[MatchingRule] Matched by PAN");
      return MatchResult.success(byPan.get().getCustomerId());
    }

    Optional<CustomerDTO> byEmail = email != null && !email.isEmpty() ? searchByEmail(email) : Optional.empty();
    if (byEmail.isPresent() && verifyMatch(byEmail.get(), customerName, dob)) {
      log.debug("[MatchingRule] Matched by email");
      return MatchResult.success(byEmail.get().getCustomerId());
    }

    // 2. Composite: if we have multiple identifiers, require DOB or name verification for any candidate
    if (byMobile.isPresent() && (dob != null || customerName != null)) {
      if (verifyMatch(byMobile.get(), customerName, dob)) return MatchResult.success(byMobile.get().getCustomerId());
    }
    if (byPan.isPresent() && (dob != null || customerName != null)) {
      if (verifyMatch(byPan.get(), customerName, dob)) return MatchResult.success(byPan.get().getCustomerId());
    }
    if (byEmail.isPresent() && (dob != null || customerName != null)) {
      if (verifyMatch(byEmail.get(), customerName, dob)) return MatchResult.success(byEmail.get().getCustomerId());
    }

    // 3. Accept first found if no DOB/name to verify (we already tried above)
    if (byMobile.isPresent()) return MatchResult.success(byMobile.get().getCustomerId());
    if (byPan.isPresent()) return MatchResult.success(byPan.get().getCustomerId());
    if (byEmail.isPresent()) return MatchResult.success(byEmail.get().getCustomerId());

    // 4. No match - determine reason
    boolean foundCustomer = byMobile.isPresent() || byPan.isPresent() || byEmail.isPresent();
    return MatchResult.failure(foundCustomer
        ? "Verification failed: name or DOB mismatch"
        : "No customer found (mobile/email/PAN)");
  }

  private boolean verifyMatch(CustomerDTO customer, String customerName, String dob) {
    if (customerName != null && !customerName.isBlank()) {
      String dbName = (customer.getFirstName() + " " + customer.getLastName()).trim().toLowerCase();
      String csvName = customerName.trim().toLowerCase();
      if (dbName.isEmpty()) return true;
      int d = levenshteinDistance.apply(csvName, dbName);
      if (d > NAME_SIMILARITY_THRESHOLD) return false;
    }
    if (dob != null && !dob.isBlank() && customer.getDateOfBirth() != null) {
      String dbDob = customer.getDateOfBirth().length() >= 10 ? customer.getDateOfBirth().substring(0, 10) : customer.getDateOfBirth();
      String csvDob = dob.length() >= 10 ? dob.substring(0, 10) : dob.replaceAll("[^0-9]", "");
      if (csvDob.length() >= 8 && !dbDob.replaceAll("[^0-9]", "").contains(csvDob) && !csvDob.contains(dbDob.replaceAll("[^0-9]", "")))
        return false;
    }
    return true;
  }

  private Optional<CustomerDTO> searchByMobile(String mobile) {
    try {
      return customerClient.searchByMobile(mobile);
    } catch (FeignException.NotFound e) {
      return Optional.empty();
    } catch (FeignException e) {
      log.warn("[MatchingRule] Customer client error (mobile): {}", e.getMessage());
      return Optional.empty();
    }
  }

  private Optional<CustomerDTO> searchByEmail(String email) {
    try {
      return customerClient.searchByEmail(email);
    } catch (FeignException.NotFound e) {
      return Optional.empty();
    } catch (FeignException e) {
      log.warn("[MatchingRule] Customer client error (email): {}", e.getMessage());
      return Optional.empty();
    }
  }

  private Optional<CustomerDTO> searchByPan(String pan) {
    try {
      return customerClient.searchByPan(pan);
    } catch (FeignException.NotFound e) {
      return Optional.empty();
    } catch (FeignException e) {
      log.warn("[MatchingRule] Customer client error (pan): {}", e.getMessage());
      return Optional.empty();
    }
  }

  private static String normalize(String s) {
    return s != null ? s.trim() : null;
  }
}
