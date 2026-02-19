package com.mypolicy.pipeline.ingestion.controller;

import com.mypolicy.pipeline.ingestion.dto.JobStatusResponse;
import com.mypolicy.pipeline.ingestion.dto.ProgressUpdateRequest;
import com.mypolicy.pipeline.ingestion.dto.StatusUpdateRequest;
import com.mypolicy.pipeline.ingestion.dto.UploadResponse;
import com.mypolicy.pipeline.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Ingestion API: file upload, status retrieval, progress/status updates.
 * JWT validation is done at BFF level for upload.
 * 
 * Consolidated Service: Part of data-pipeline-service on port 8082.
 */
@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

  private static final Logger log = LoggerFactory.getLogger(IngestionController.class);
  private final IngestionService ingestionService;

  /**
   * POST /api/v1/ingestion/upload
   * Accepts Excel (.xls, .xlsx) or CSV (.csv) files, validates, stores, creates job.
   */
  @PostMapping("/upload")
  public ResponseEntity<UploadResponse> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam("insurerId") String insurerId,
      @RequestParam("uploadedBy") String uploadedBy,
      @RequestParam(value = "fileType", required = false) String fileType) {

    log.info("[Ingestion API] POST /upload - insurerId={}, uploadedBy={}", insurerId, uploadedBy);

    try {
<<<<<<< HEAD:MyPolicy-Backend/ingestion-service/src/main/java/com/mypolicy/ingestion/controller/IngestionController.java
      UploadResponse response = ingestionService.uploadFile(file, insurerId, uploadedBy, fileType);
=======
      UploadResponse response = ingestionService.uploadFile(file, insurerId, uploadedBy);
      log.info("[Ingestion API] Upload successful: jobId={}", response.getJobId());
>>>>>>> upstream/main:MyPolicy-Backend/data-pipeline-service/src/main/java/com/mypolicy/pipeline/ingestion/controller/IngestionController.java
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      log.warn("[Ingestion API] Upload validation failed: {}", e.getMessage());
      throw e;
    } catch (IOException e) {
      log.error("[Ingestion API] File storage failed", e);
      throw new RuntimeException("Error storing file", e);
    }
  }

  /**
   * GET /api/v1/ingestion/status/{jobId}
   * Returns job status for BFF UI and Processing Service.
   */
  @GetMapping("/status/{jobId}")
  public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
    log.debug("[Ingestion API] GET /status/{}", jobId);
    JobStatusResponse response = ingestionService.getJobStatus(jobId);
    return ResponseEntity.ok(response);
  }

  /**
   * PATCH /api/v1/ingestion/{jobId}/progress
   * Internal: Processing Service updates processed record count.
   * Idempotent when retried.
   */
  @PatchMapping("/{jobId}/progress")
  public ResponseEntity<Void> updateProgress(
      @PathVariable String jobId,
      @Valid @RequestBody ProgressUpdateRequest request) {

    log.debug("[Ingestion API] PATCH /{}/progress - delta={}", jobId, request.getProcessedRecordsDelta());
    ingestionService.updateProgress(jobId, request);
    return ResponseEntity.noContent().build();
  }

  /**
   * PATCH /api/v1/ingestion/{jobId}/status
   * Internal: Processing Service transitions job state.
   * Allowed: UPLOADED→PROCESSING, PROCESSING→COMPLETED|FAILED
   */
  @PatchMapping("/{jobId}/status")
  public ResponseEntity<Void> updateStatus(
      @PathVariable String jobId,
      @Valid @RequestBody StatusUpdateRequest request) {

    log.info("[Ingestion API] PATCH /{}/status - newStatus={}", jobId, request.getStatus());
    ingestionService.updateStatus(jobId, request);
    return ResponseEntity.noContent().build();
  }

  /**
   * Health check endpoint.
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Ingestion module healthy");
  }
}
