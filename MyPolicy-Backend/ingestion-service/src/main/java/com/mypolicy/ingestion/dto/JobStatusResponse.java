package com.mypolicy.ingestion.dto;

import com.mypolicy.ingestion.model.IngestionStatus;

import java.time.LocalDateTime;

/**
 * Job status response for BFF/Processing Service.
 */
public class JobStatusResponse {
  private String jobId;
  private IngestionStatus status;
  private int processedRecords;
  private int totalRecords;
  private String filePath;
  private String insurerId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public JobStatusResponse() {
  }

  public JobStatusResponse(String jobId, IngestionStatus status, int processedRecords,
      int totalRecords, String filePath, String insurerId, LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.jobId = jobId;
    this.status = status;
    this.processedRecords = processedRecords;
    this.totalRecords = totalRecords;
    this.filePath = filePath;
    this.insurerId = insurerId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getJobId() { return jobId; }
  public void setJobId(String jobId) { this.jobId = jobId; }
  public IngestionStatus getStatus() { return status; }
  public void setStatus(IngestionStatus status) { this.status = status; }
  public int getProcessedRecords() { return processedRecords; }
  public void setProcessedRecords(int processedRecords) { this.processedRecords = processedRecords; }
  public int getTotalRecords() { return totalRecords; }
  public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public String getInsurerId() { return insurerId; }
  public void setInsurerId(String insurerId) { this.insurerId = insurerId; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
