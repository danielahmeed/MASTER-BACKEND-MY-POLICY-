package com.mypolicy.ingestion.dto;

import com.mypolicy.ingestion.model.IngestionStatus;

/**
 * Response after successful file upload.
 */
public class UploadResponse {
  private String jobId;
  private IngestionStatus status;

  public UploadResponse() {
  }

  public UploadResponse(String jobId, IngestionStatus status) {
    this.jobId = jobId;
    this.status = status;
  }

  public String getJobId() { return jobId; }
  public void setJobId(String jobId) { this.jobId = jobId; }
  public IngestionStatus getStatus() { return status; }
  public void setStatus(IngestionStatus status) { this.status = status; }
}
