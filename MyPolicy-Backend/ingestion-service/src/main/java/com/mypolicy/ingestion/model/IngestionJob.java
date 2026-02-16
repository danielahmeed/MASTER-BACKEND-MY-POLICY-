package com.mypolicy.ingestion.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Ingestion job document - tracks file upload and processing lifecycle.
 * Stateless service: all state lives in MongoDB.
 */
@Document(collection = "ingestion_jobs")
public class IngestionJob {

  @Id
  private String jobId;

  private String insurerId;
  private String filePath;

  @Indexed
  private IngestionStatus status;
  private int totalRecords;
  private int processedRecords;
  private String uploadedBy;
  private String failureReason;

  @Indexed
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public IngestionJob() {
  }

  public IngestionJob(String jobId, String insurerId, String filePath, IngestionStatus status,
      int totalRecords, int processedRecords, String uploadedBy, String failureReason,
      LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.jobId = jobId;
    this.insurerId = insurerId;
    this.filePath = filePath;
    this.status = status;
    this.totalRecords = totalRecords;
    this.processedRecords = processedRecords;
    this.uploadedBy = uploadedBy;
    this.failureReason = failureReason;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getJobId() { return jobId; }
  public void setJobId(String jobId) { this.jobId = jobId; }
  public String getInsurerId() { return insurerId; }
  public void setInsurerId(String insurerId) { this.insurerId = insurerId; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public IngestionStatus getStatus() { return status; }
  public void setStatus(IngestionStatus status) { this.status = status; }
  public int getTotalRecords() { return totalRecords; }
  public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
  public int getProcessedRecords() { return processedRecords; }
  public void setProcessedRecords(int processedRecords) { this.processedRecords = processedRecords; }
  public String getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
  public String getFailureReason() { return failureReason; }
  public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
