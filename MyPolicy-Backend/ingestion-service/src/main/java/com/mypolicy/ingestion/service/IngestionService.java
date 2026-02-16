package com.mypolicy.ingestion.service;

import com.mypolicy.ingestion.dto.JobStatusResponse;
import com.mypolicy.ingestion.dto.ProgressUpdateRequest;
import com.mypolicy.ingestion.dto.StatusUpdateRequest;
import com.mypolicy.ingestion.dto.UploadResponse;
import com.mypolicy.ingestion.model.IngestionJob;
import com.mypolicy.ingestion.model.IngestionStatus;
import com.mypolicy.ingestion.repository.IngestionJobRepository;
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
 */
@Service
public class IngestionService {

  private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".xls", ".xlsx", ".csv");
  private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB

  private final IngestionJobRepository jobRepository;

  public IngestionService(IngestionJobRepository jobRepository) {
    this.jobRepository = jobRepository;
  }

  @Value("${ingestion.storage.path:storage/ingestion}")
  private String storageBasePath;

  /**
   * Upload file (Excel or CSV), persist to storage, create job record.
   */
  public UploadResponse uploadFile(MultipartFile file, String insurerId, String uploadedBy)
      throws IOException {

    // 1. Validate file
    validateFile(file);

    // 2. Generate jobId and save file
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

    log.info("File uploaded: jobId={}, insurerId={}, path={}", jobId, insurerId,
        filePath.toAbsolutePath());

    // 3. Create ingestion job
    IngestionJob job = new IngestionJob(jobId, insurerId, filePath.toAbsolutePath().toString(),
        IngestionStatus.UPLOADED, 0, 0, uploadedBy, null, LocalDateTime.now(), LocalDateTime.now());

    jobRepository.save(job);

    log.info("Ingestion job created: jobId={}, status=UPLOADED", jobId);

    return new UploadResponse(jobId, IngestionStatus.UPLOADED);
  }

  /**
   * Get job status for BFF/Processing.
   */
  public JobStatusResponse getJobStatus(String jobId) {
    IngestionJob job = jobRepository.findById(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

    return new JobStatusResponse(job.getJobId(), job.getStatus(), job.getProcessedRecords(),
        job.getTotalRecords(), job.getFilePath(), job.getInsurerId(), job.getCreatedAt(),
        job.getUpdatedAt());
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

    log.debug("Progress updated: jobId={}, processed={}/{}", jobId, job.getProcessedRecords(),
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

    log.info("Status transition: jobId={}, {} -> {}", jobId, oldStatus, newStatus);
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

    log.debug("Total records set: jobId={}, total={}", jobId, totalRecords);
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
