package com.mypolicy.ingestion.dto;

/**
 * Internal request for incrementing processed records.
 * Idempotent when retried with same delta.
 */
public class ProgressUpdateRequest {
  private int processedRecordsDelta;

  public int getProcessedRecordsDelta() { return processedRecordsDelta; }
  public void setProcessedRecordsDelta(int processedRecordsDelta) {
    this.processedRecordsDelta = processedRecordsDelta;
  }
}
