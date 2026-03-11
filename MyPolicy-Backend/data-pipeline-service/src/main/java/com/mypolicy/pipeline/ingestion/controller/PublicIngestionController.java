package com.mypolicy.pipeline.ingestion.controller;

import com.mypolicy.pipeline.ingestion.dto.JobStatusResponse;
import com.mypolicy.pipeline.ingestion.dto.UploadResponse;
import com.mypolicy.pipeline.ingestion.model.IngestionJob;
import com.mypolicy.pipeline.ingestion.service.IngestionService;
import com.mypolicy.pipeline.processing.service.ProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Public API for insurers: upload CSV/Excel, trigger processing, check job status.
 */
@RestController
@RequestMapping("/api/public/v1/ingestion")
public class PublicIngestionController {

  private static final Logger log = LoggerFactory.getLogger(PublicIngestionController.class);
  private final IngestionService ingestionService;
  private final ProcessingService processingService;

  public PublicIngestionController(IngestionService ingestionService, ProcessingService processingService) {
    this.ingestionService = ingestionService;
    this.processingService = processingService;
  }

  /**
   * Upload CSV/Excel for ingestion. Requires X-API-Key header.
   */
  @PostMapping("/upload")
  public ResponseEntity<UploadResponse> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam("insurerId") String insurerId,
      @RequestParam("uploadedBy") String uploadedBy,
      @RequestParam(value = "fileType", required = false) String fileType) {

    try {
      UploadResponse response = ingestionService.uploadFile(file, insurerId, uploadedBy, fileType);
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
   * List all ingestion jobs (view MongoDB ingestion_jobs data).
   * GET /api/public/v1/ingestion/jobs
   */
  @GetMapping("/jobs")
  public ResponseEntity<?> listJobs() {
    return ResponseEntity.ok(ingestionService.listAllJobs());
  }

  /**
   * Get job status.
   */
  @GetMapping("/status/{jobId}")
  public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
    JobStatusResponse response = ingestionService.getJobStatus(jobId);
    return ResponseEntity.ok(response);
  }

  /**
   * Trigger processing of an uploaded file. Call after upload with the returned jobId.
   * policyType is optional (e.g. HEALTH, MOTOR, TERM_LIFE); resolved from config if omitted.
   */
  @PostMapping("/process/{jobId}")
  public ResponseEntity<Map<String, Object>> triggerProcessing(
      @PathVariable String jobId,
      @RequestParam(value = "policyType", required = false) String policyType) {
    IngestionJob job = ingestionService.getJob(jobId);
    if (job.getStatus() != com.mypolicy.pipeline.ingestion.model.IngestionStatus.UPLOADED) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Job must be in UPLOADED state", "currentStatus", job.getStatus().name()));
    }
    try {
      processingService.processFile(jobId, job.getFilePath(), job.getInsurerId(), policyType);
      return ResponseEntity.accepted()
          .body(Map.of("jobId", jobId, "message", "Processing started", "status", "PROCESSING"));
    } catch (Exception e) {
      log.error("Processing failed for jobId={}", jobId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Processing failed", "detail", e.getMessage()));
    }
  }
}
