package com.mypolicy.ingestion.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "ingestion_jobs")
public class IngestionJob {

  @Id
  private String jobId;

  private String fileName;
  private String fileKey; // S3 key or local path for now
  private String insurerId;
  private String policyType;
  private long fileSize;
  private String contentType;

  // Status
  private IngestionStatus status;
  private int totalRecords;
  private int processedRecords;
  private int successRecords;
  private int failedRecords;

  private List<ValidationError> validationErrors;

  // Timestamps
  private LocalDateTime uploadedAt;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private String uploadedBy; // Admin email/ID
}
