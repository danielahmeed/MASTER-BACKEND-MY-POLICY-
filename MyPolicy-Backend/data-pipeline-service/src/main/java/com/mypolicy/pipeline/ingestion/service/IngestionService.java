package com.mypolicy.pipeline.ingestion.service;

<<<<<<< HEAD:MyPolicy-Backend/ingestion-service/src/main/java/com/mypolicy/ingestion/service/IngestionService.java
import com.mypolicy.ingestion.dto.JobStatusResponse;
import com.mypolicy.ingestion.dto.ProgressUpdateRequest;
import com.mypolicy.ingestion.dto.StatusUpdateRequest;
import com.mypolicy.ingestion.dto.UploadResponse;
import com.mypolicy.ingestion.model.IngestionJob;
import com.mypolicy.ingestion.model.IngestionStatus;
import com.mypolicy.ingestion.repository.IngestionJobRepository;
import com.mypolicy.ingestion.validation.InsurerSchemaValidator;
import com.mypolicy.ingestion.validation.SchemaValidationResult;
=======
import com.mypolicy.pipeline.ingestion.dto.JobStatusResponse;
import com.mypolicy.pipeline.ingestion.dto.ProgressUpdateRequest;
import com.mypolicy.pipeline.ingestion.dto.StatusUpdateRequest;
import com.mypolicy.pipeline.ingestion.dto.UploadResponse;
import com.mypolicy.pipeline.ingestion.model.IngestionJob;
import com.mypolicy.pipeline.ingestion.model.IngestionStatus;
import com.mypolicy.pipeline.ingestion.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
>>>>>>> upstream/main:MyPolicy-Backend/data-pipeline-service/src/main/java/com/mypolicy/pipeline/ingestion/service/IngestionService.java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Ingestion Service: file intake, validation, storage, and job lifecycle.
 * Does NOT parse, transform, or process policy data.
 * 
 * Consolidated Service: Part of data-pipeline-service, now called directly by Processing module.
 */
@Service
@RequiredArgsConstructor
public class IngestionService {

  private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".xls", ".xlsx", ".csv");
  private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB

  private final IngestionJobRepository jobRepository;
  private final InsurerSchemaValidator schemaValidator;

<<<<<<< HEAD:MyPolicy-Backend/ingestion-service/src/main/java/com/mypolicy/ingestion/service/IngestionService.java
  public IngestionService(IngestionJobRepository jobRepository, InsurerSchemaValidator schemaValidator) {
    this.jobRepository = jobRepository;
    this.schemaValidator = schemaValidator;
  }

=======
>>>>>>> upstream/main:MyPolicy-Backend/data-pipeline-service/src/main/java/com/mypolicy/pipeline/ingestion/service/IngestionService.java
  @Value("${ingestion.storage.path:storage/ingestion}")
  private String storageBasePath;

  @Value("${ingestion.schema.validate:true}")
  private boolean schemaValidationEnabled;

  /**
   * Upload file (Excel or CSV), persist to storage, create job record.
   * @param fileType "correction" for delta/correction files (triggers UPDATE); "normal" (default) for new data.
   */
  public UploadResponse uploadFile(MultipartFile file, String insurerId, String uploadedBy,
      String fileType) throws IOException {

    log.info("[Ingestion] Starting file upload: insurerId={}, filename={}", insurerId, file.getOriginalFilename());

    // 1. Validate file
    validateFile(file);

    // 2. Detect file type: param, or filename *_correction.csv
    String resolvedFileType = resolveFileType(fileType, file.getOriginalFilename());

    // 3. Schema validation for normal files (skip for correction files)
    if (schemaValidationEnabled && "normal".equals(resolvedFileType)) {
      SchemaValidationResult schemaResult = schemaValidator.validate(file, insurerId);
      if (!schemaResult.isValid()) {
        throw new IllegalArgumentException(schemaResult.getErrorSummary());
      }
    }

    // 4. Generate jobId and save file
    String jobId = UUID.randomUUID().toString();
    String extension = getFileExtension(file.getOriginalFilename());
    Path storagePath = Paths.get(storageBasePath);
    if (!Files.exists(storagePath)) {
      Files.createDirectories(storagePath);
    }

    Path filePath = storagePath.resolve(jobId + extension);
    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, filePath);
    }

<<<<<<< HEAD:MyPolicy-Backend/ingestion-service/src/main/java/com/mypolicy/ingestion/service/IngestionService.java
    log.info("File uploaded: jobId={}, insurerId={}, fileType={}, path={}", jobId, insurerId,
        resolvedFileType, filePath.toAbsolutePath());
=======
    log.info("[Ingestion] File uploaded: jobId={}, insurerId={}, path={}", jobId, insurerId,
        filePath.toAbsolutePath());
>>>>>>> upstream/main:MyPolicy-Backend/data-pipeline-service/src/main/java/com/mypolicy/pipeline/ingestion/service/IngestionService.java

    // 5. Create ingestion job
    IngestionJob job = new IngestionJob(jobId, insurerId, filePath.toAbsolutePath().toString(),
        resolvedFileType, IngestionStatus.UPLOADED, 0, 0, uploadedBy, null,
        LocalDateTime.now(), LocalDateTime.now());

    jobRepository.save(job);

    log.info("[Ingestion] Job created: jobId={}, status=UPLOADED", jobId);

    return new UploadResponse(jobId, IngestionStatus.UPLOADED);
  }

  /**
   * Get job status for BFF/Processing.
   */
  public JobStatusResponse getJobStatus(String jobId) {
    log.debug("[Ingestion] Fetching job status: jobId={}", jobId);
    
    IngestionJob job = jobRepository.findById(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

    return new JobStatusResponse(job.getJobId(), job.getStatus(), job.getProcessedRecords(),
        job.getTotalRecords(), job.getFilePath(), job.getInsurerId(), job.getFileType(),
        job.getCreatedAt(), job.getUpdatedAt());
  }

  /**
   * Internal: get job entity (for Processing module's direct method calls).
   */
  public IngestionJob getJob(String jobId) {
    return jobRepository.findById(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
  }

  /**
   * Internal: increment processed records (idempotent when retried).
   */
  public void updateProgress(String jobId, ProgressUpdateRequest request) {
    if (request.getProcessedRecordsDelta() <= 0) {
      throw new IllegalArgumentException("processedRecordsDelta must be positive");
    }

    IngestionJob job = jobRepository.findById(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

    if (job.getStatus() != IngestionStatus.PROCESSING) {
      throw new IllegalStateException(
          "Cannot update progress: job must be in PROCESSING state, current=" + job.getStatus());
    }

    job.setProcessedRecords(job.getProcessedRecords() + request.getProcessedRecordsDelta());
    job.setUpdatedAt(LocalDateTime.now());
    jobRepository.save(job);

    log.debug("[Ingestion] Progress updated: jobId={}, processed={}/{}", jobId, job.getProcessedRecords(),
        job.getTotalRecords());
  }

  /**
   * Internal: transition job status. Enforces state machine.
   */
  public void updateStatus(String jobId, StatusUpdateRequest request) {
    IngestionJob job = jobRepository.findById(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

    IngestionStatus newStatus = request.getStatus();
    if (newStatus == null) {
      throw new IllegalArgumentException("status is required");
    }

    IngestionStatus oldStatus = job.getStatus();
    validateStateTransition(oldStatus, newStatus);

    job.setStatus(newStatus);
    job.setFailureReason(request.getFailureReason());
    job.setUpdatedAt(LocalDateTime.now());
    jobRepository.save(job);

    log.info("[Ingestion] Status transition: jobId={}, {} -> {}", jobId, oldStatus, newStatus);
  }

  /**
   * Update total records (e.g. when Processing Service determines count).
   */
  public void setTotalRecords(String jobId, int totalRecords) {
    IngestionJob job = jobRepository.findById(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

    if (job.getStatus() != IngestionStatus.UPLOADED && job.getStatus() != IngestionStatus.PROCESSING) {
      throw new IllegalStateException("Cannot set totalRecords in state: " + job.getStatus());
    }

    job.setTotalRecords(totalRecords);
    job.setUpdatedAt(LocalDateTime.now());
    jobRepository.save(job);

    log.debug("[Ingestion] Total records set: jobId={}, total={}", jobId, totalRecords);
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File is empty or missing");
    }

    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new IllegalArgumentException(
          "File size exceeds maximum allowed: " + (MAX_FILE_SIZE_BYTES / 1024 / 1024) + "MB");
    }

    String originalName = file.getOriginalFilename();
    if (originalName == null || originalName.isBlank()) {
      throw new IllegalArgumentException("File name is missing");
    }

    String ext = getFileExtension(originalName);
    if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
      throw new IllegalArgumentException(
          "Invalid file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
    }
  }

  private String getFileExtension(String filename) {
    if (filename == null)
      return null;
    int lastDot = filename.lastIndexOf('.');
    return lastDot > 0 ? filename.substring(lastDot) : null;
  }

  /** Resolve fileType: use param if provided, else detect from filename (*_correction.csv). */
  private String resolveFileType(String fileTypeParam, String filename) {
    if (fileTypeParam != null && "correction".equalsIgnoreCase(fileTypeParam.trim()))
      return "correction";
    if (filename != null && filename.toLowerCase().contains("_correction"))
      return "correction";
    return "normal";
  }

  /**
   * State machine: UPLOADED → PROCESSING → COMPLETED | FAILED
   * No backward transitions. No skipping states.
   */
  private void validateStateTransition(IngestionStatus current, IngestionStatus next) {
    switch (current) {
      case UPLOADED:
        if (next != IngestionStatus.PROCESSING) {
          throw new IllegalStateException(
              "Invalid transition: UPLOADED -> " + next + ". Allowed: PROCESSING");
        }
        break;
      case PROCESSING:
        if (next != IngestionStatus.COMPLETED && next != IngestionStatus.FAILED) {
          throw new IllegalStateException(
              "Invalid transition: PROCESSING -> " + next + ". Allowed: COMPLETED, FAILED");
        }
        break;
      case COMPLETED:
      case FAILED:
        throw new IllegalStateException(
            "No transitions allowed from terminal state: " + current);
      default:
        throw new IllegalStateException("Unknown status: " + current);
    }
  }
}
