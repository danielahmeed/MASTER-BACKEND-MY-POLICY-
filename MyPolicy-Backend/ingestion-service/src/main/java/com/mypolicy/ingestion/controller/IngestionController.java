package com.mypolicy.ingestion.controller;

import com.mypolicy.ingestion.dto.JobStatusResponse;
import com.mypolicy.ingestion.dto.ProgressUpdateRequest;
import com.mypolicy.ingestion.dto.StatusUpdateRequest;
import com.mypolicy.ingestion.dto.UploadResponse;
import com.mypolicy.ingestion.service.IngestionService;
import jakarta.validation.Valid;
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
 */
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

  private static final Logger log = LoggerFactory.getLogger(IngestionController.class);
  private final IngestionService ingestionService;

  public IngestionController(IngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  /**
   * POST /api/v1/ingestion/upload
   * Accepts Excel (.xls, .xlsx) or CSV (.csv) files, validates, stores, creates job.
   */
  @PostMapping("/upload")
  public ResponseEntity<UploadResponse> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam("insurerId") String insurerId,
      @RequestParam("uploadedBy") String uploadedBy) {

    try {
      UploadResponse response = ingestionService.uploadFile(file, insurerId, uploadedBy);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      log.warn("Upload validation failed: {}", e.getMessage());
      throw e;
    } catch (IOException e) {
      log.error("File storage failed", e);
      throw new RuntimeException("Error storing file", e);
    }
  }

  /**
   * GET /api/v1/ingestion/status/{jobId}
   * Returns job status for BFF UI and Processing Service.
   */
  @GetMapping("/status/{jobId}")
  public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
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

    ingestionService.updateStatus(jobId, request);
    return ResponseEntity.noContent().build();
  }
}
