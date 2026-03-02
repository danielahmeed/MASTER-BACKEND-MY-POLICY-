package com.mypolicy.pipeline.ingestion.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ingestion job entity - tracks file upload and processing lifecycle.
 * Stored in H2 (JPA) for compatibility without MongoDB.
 *
 * Consolidated Service: Part of data-pipeline-service.
 */
@Entity
@Table(name = "ingestion_jobs")
public class IngestionJob {

  @Id
  @Column(name = "job_id", length = 64)
  private String jobId;

  @Column(name = "insurer_id", nullable = false)
  private String insurerId;

  @Column(name = "file_path", length = 1024)
  private String filePath;

  @Column(name = "file_type", length = 32)
  private String fileType; // "normal" or "correction"

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private IngestionStatus status;

  @Column(name = "total_records")
  private int totalRecords;

  @Column(name = "processed_records")
  private int processedRecords;

  @Column(name = "uploaded_by", length = 256)
  private String uploadedBy;

  @Column(name = "failure_reason", length = 2048)
  private String failureReason;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Type(JsonType.class)
  @Column(name = "verification_failures", columnDefinition = "varchar(65535)")
  private List<Map<String, String>> verificationFailures = new ArrayList<>();

  public IngestionJob() {
  }

  public IngestionJob(String jobId, String insurerId, String filePath, String fileType, IngestionStatus status,
      int totalRecords, int processedRecords, String uploadedBy, String failureReason,
      LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.jobId = jobId;
    this.insurerId = insurerId;
    this.filePath = filePath;
    this.fileType = fileType;
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
  public String getFileType() { return fileType; }
  public void setFileType(String fileType) { this.fileType = fileType; }
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
  public List<Map<String, String>> getVerificationFailures() { return verificationFailures; }
  public void setVerificationFailures(List<Map<String, String>> verificationFailures) {
    this.verificationFailures = verificationFailures != null ? verificationFailures : new ArrayList<>();
  }
}
