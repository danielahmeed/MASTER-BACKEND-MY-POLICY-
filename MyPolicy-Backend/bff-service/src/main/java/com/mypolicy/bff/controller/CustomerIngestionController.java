package com.mypolicy.bff.controller;

import com.mypolicy.bff.client.IngestionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unified customer-facing ingestion API.
 * External customers use this endpoint to upload CSV/Excel files for processing.
 * Requires X-API-Key header for authentication.
 */
@RestController
@RequestMapping("/api/public/v1/ingestion")
@RequiredArgsConstructor
public class CustomerIngestionController {

  private final IngestionClient ingestionClient;

  /**
   * Upload a CSV or Excel file for ingestion.
   * Allowed formats: .csv, .xls, .xlsx (max 50MB).
   *
   * @param file       The file to upload
   * @param insurerId  Insurer or customer identifier (e.g. HDFC_LIFE, customer-org-id)
   * @param uploadedBy Identifier of the uploader (user/system ID)
   */
  @PostMapping("/upload")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<Object> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam("insurerId") String insurerId,
      @RequestParam("uploadedBy") String uploadedBy) {

    Object response = ingestionClient.uploadFile(file, insurerId, uploadedBy);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Get the status of an ingestion job.
   *
   * @param jobId The job ID returned from upload
   */
  @GetMapping("/status/{jobId}")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<Object> getJobStatus(@PathVariable String jobId) {
    return ResponseEntity.ok(ingestionClient.getJobStatus(jobId));
  }
}
