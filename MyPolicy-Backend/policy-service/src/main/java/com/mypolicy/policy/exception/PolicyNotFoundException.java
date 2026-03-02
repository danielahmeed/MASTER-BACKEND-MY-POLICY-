package com.mypolicy.policy.exception;

/**
 * Exception thrown when a policy is not found
 */
public class PolicyNotFoundException extends RuntimeException {
  
  public PolicyNotFoundException(String message) {
    super(message);
  }

  public PolicyNotFoundException(String policyId, String field) {
    super(String.format("Policy not found with %s: %s", field, policyId));
  }
}
