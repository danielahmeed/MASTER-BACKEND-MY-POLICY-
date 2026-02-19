package com.mypolicy.ingestion.controller;

import com.mypolicy.ingestion.dto.JobStatusResponse;
import com.mypolicy.ingestion.dto.UploadResponse;
import com.mypolicy.ingestion.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Exposed customer-facing ingestion API.
 * Requires X-API-Key header (validated by ApiKeyAuthFilter).
 * Delegates to IngestionService - no business logic here.
 */
@RestController
@RequestMapping("/api/public/v1/ingestion")
public class PublicIngestionController {

  private static final Logger log = LoggerFactory.getLogger(PublicIngestionController.class);
  private final IngestionService ingestionService;

  public PublicIngestionController(IngestionService ingestionService) {
    this.ingestionService = ingestionService;
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
   * Get job status. Requires X-API-Key header.
   */
  @GetMapping("/status/{jobId}")
  public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
    JobStatusResponse response = ingestionService.getJobStatus(jobId);
    return ResponseEntity.ok(response);
  }
}
