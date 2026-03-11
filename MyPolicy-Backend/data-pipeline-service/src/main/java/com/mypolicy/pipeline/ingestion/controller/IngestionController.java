package com.mypolicy.pipeline.ingestion.controller;

import com.mypolicy.pipeline.common.dto.ApiResponse;
import com.mypolicy.pipeline.common.security.JwtUtil;
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
 * JWT validation is performed for user authentication.
 * All responses follow the standard ApiResponse wrapper format.
 * 
 * Consolidated Service: Part of data-pipeline-service on port 8082.
 */
@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

  private static final Logger log = LoggerFactory.getLogger(IngestionController.class);
  private final IngestionService ingestionService;
  private final JwtUtil jwtUtil;

  public IngestionController(IngestionService ingestionService, JwtUtil jwtUtil) {
    this.ingestionService = ingestionService;
    this.jwtUtil = jwtUtil;
  }

  /**
   * POST /api/v1/ingestion/upload
   * Accepts Excel (.xls, .xlsx) or CSV (.csv) files, validates, stores, creates
   * job.
   * Extracts uploadedBy from JWT token in Authorization header.
   */
  @PostMapping("/upload")
  public ResponseEntity<ApiResponse<UploadResponse>> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam("insurerId") String insurerId,
      @RequestHeader("Authorization") String authorizationHeader,
      @RequestParam(value = "fileType", required = false) String fileType) {

    try {
      String uploadedBy = jwtUtil.extractUsernameFromHeader(authorizationHeader);
      log.info("[Ingestion API] POST /upload - insurerId={}, uploadedBy={}", insurerId, uploadedBy);

      UploadResponse response = ingestionService.uploadFile(file, insurerId, uploadedBy, fileType);
      log.info("[Ingestion API] Upload successful: jobId={}", response.getJobId());

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, "File uploaded successfully"));
    } catch (IllegalArgumentException e) {
      log.warn("[Ingestion API] Upload validation failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR"));
    } catch (IOException e) {
      log.error("[Ingestion API] File storage failed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error storing file", "STORAGE_ERROR", e.getMessage()));
    } catch (Exception e) {
      log.error("[Ingestion API] Unexpected error during upload", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Unexpected error during upload", "INTERNAL_ERROR", e.getMessage()));
    }
  }

  /**
   * GET /api/v1/ingestion/status/{jobId}
   * Returns job status for BFF UI and Processing Service.
   */
  @GetMapping("/status/{jobId}")
  public ResponseEntity<ApiResponse<JobStatusResponse>> getJobStatus(@PathVariable String jobId) {
    try {
      log.debug("[Ingestion API] GET /status/{}", jobId);
      JobStatusResponse response = ingestionService.getJobStatus(jobId);
      return ResponseEntity.ok(ApiResponse.success(response, "Job status retrieved successfully"));
    } catch (IllegalArgumentException e) {
      log.warn("[Ingestion API] Job not found: {}", jobId);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error(e.getMessage(), "JOB_NOT_FOUND"));
    } catch (Exception e) {
      log.error("[Ingestion API] Error retrieving job status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error retrieving job status", "INTERNAL_ERROR", e.getMessage()));
    }
  }

  /**
   * PATCH /api/v1/ingestion/{jobId}/progress
   * Internal: Processing Service updates processed record count.
   * Idempotent when retried.
   * 
   * NOTE: This endpoint is distinct from updateStatus:
   * - updateProgress: Increments processed records counter (used during
   * processing)
   * - updateStatus: Transitions job state (UPLOADED -> PROCESSING ->
   * COMPLETED/FAILED)
   */
  @PatchMapping("/{jobId}/progress")
  public ResponseEntity<ApiResponse<Void>> updateProgress(
      @PathVariable String jobId,
      @Valid @RequestBody ProgressUpdateRequest request) {

    try {
      log.debug("[Ingestion API] PATCH /{}/progress - delta={}", jobId, request.getProcessedRecordsDelta());
      ingestionService.updateProgress(jobId, request);
      return ResponseEntity.ok(ApiResponse.success("Progress updated successfully"));
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.warn("[Ingestion API] Progress update failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_PROGRESS_UPDATE"));
    } catch (Exception e) {
      log.error("[Ingestion API] Error updating progress", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error updating progress", "INTERNAL_ERROR", e.getMessage()));
    }
  }

  /**
   * PATCH /api/v1/ingestion/{jobId}/status
   * Internal: Processing Service transitions job state.
   * Allowed: UPLOADED→PROCESSING, PROCESSING→COMPLETED|FAILED
   */
  @PatchMapping("/{jobId}/status")
  public ResponseEntity<ApiResponse<Void>> updateStatus(
      @PathVariable String jobId,
      @Valid @RequestBody StatusUpdateRequest request) {

    try {
      log.info("[Ingestion API] PATCH /{}/status - newStatus={}", jobId, request.getStatus());
      ingestionService.updateStatus(jobId, request);
      return ResponseEntity.ok(ApiResponse.success("Job status updated successfully"));
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.warn("[Ingestion API] Status update failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_STATUS_TRANSITION"));
    } catch (Exception e) {
      log.error("[Ingestion API] Error updating status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error updating status", "INTERNAL_ERROR", e.getMessage()));
    }
  }

  /**
   * Health check endpoint.
   */
  @GetMapping("/health")
  public ResponseEntity<ApiResponse<String>> health() {
    return ResponseEntity.ok(ApiResponse.success("healthy", "Ingestion module is operational"));
  }
}
