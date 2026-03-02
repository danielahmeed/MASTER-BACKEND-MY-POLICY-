package com.mypolicy.policy.exception;

/**
 * Exception thrown when attempting to create a policy with duplicate policy number
 */
public class DuplicatePolicyException extends RuntimeException {
  
  public DuplicatePolicyException(String message) {
    super(message);
  }

  public DuplicatePolicyException(String policyNumber, String insurerId) {
    super(String.format("Policy already exists with number %s for insurer %s", policyNumber, insurerId));
  }
}
