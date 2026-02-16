package com.mypolicy.ingestion.dto;

import com.mypolicy.ingestion.model.IngestionStatus;

/**
 * Internal request for job status transition.
 * Enforces state machine: UPLOADED → PROCESSING → COMPLETED | FAILED
 */
public class StatusUpdateRequest {
  private IngestionStatus status;
  private String failureReason;

  public IngestionStatus getStatus() { return status; }
  public void setStatus(IngestionStatus status) { this.status = status; }
  public String getFailureReason() { return failureReason; }
  public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
