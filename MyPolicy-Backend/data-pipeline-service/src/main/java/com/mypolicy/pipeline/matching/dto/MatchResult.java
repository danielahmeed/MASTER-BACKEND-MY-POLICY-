package com.mypolicy.pipeline.matching.dto;

/**
 * Result of customer matching: either customerId (success) or failureReason.
 */
public record MatchResult(String customerId, String failureReason) {
  public static MatchResult success(String customerId) {
    return new MatchResult(customerId, null);
  }
  public static MatchResult failure(String reason) {
    return new MatchResult(null, reason);
  }
  public boolean isSuccess() {
    return customerId != null;
  }
}
