package com.mypolicy.pipeline.ingestion.dto;

import com.mypolicy.pipeline.ingestion.model.IngestionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
  private String fileType;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String failureReason;
  private List<Map<String, String>> verificationFailures;

  public JobStatusResponse() {
  }

  public JobStatusResponse(String jobId, IngestionStatus status, int processedRecords,
      int totalRecords, String filePath, String insurerId, String fileType,
      LocalDateTime createdAt, LocalDateTime updatedAt, String failureReason,
      List<Map<String, String>> verificationFailures) {
    this.jobId = jobId;
    this.status = status;
    this.processedRecords = processedRecords;
    this.totalRecords = totalRecords;
    this.filePath = filePath;
    this.insurerId = insurerId;
    this.fileType = fileType;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.failureReason = failureReason;
    this.verificationFailures = verificationFailures;
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
  public String getFileType() { return fileType; }
  public void setFileType(String fileType) { this.fileType = fileType; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public String getFailureReason() { return failureReason; }
  public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
  public List<Map<String, String>> getVerificationFailures() { return verificationFailures; }
  public void setVerificationFailures(List<Map<String, String>> verificationFailures) { this.verificationFailures = verificationFailures; }
}
