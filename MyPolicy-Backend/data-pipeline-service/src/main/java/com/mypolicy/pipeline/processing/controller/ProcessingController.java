package com.mypolicy.pipeline.processing.controller;

import com.mypolicy.pipeline.processing.service.ProcessingService;
import com.mypolicy.pipeline.ingestion.service.IngestionService;
import com.mypolicy.pipeline.ingestion.model.IngestionJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Processing API: trigger file processing.
 * 
 * Consolidated Service: Part of data-pipeline-service on port 8082.
 * Note: In production, this would be triggered by Kafka events.
 */
@RestController
@RequestMapping("/api/v1/processing")
@RequiredArgsConstructor
public class ProcessingController {

  private static final Logger log = LoggerFactory.getLogger(ProcessingController.class);
  private final ProcessingService processingService;
  private final IngestionService ingestionService;

  /**
   * POST /api/v1/processing/trigger
   * Triggers processing for an uploaded file.
   * TODO: Replace with Kafka Consumer in production.
   */
  @PostMapping("/trigger")
  public ResponseEntity<String> triggerProcessing(
      @RequestParam String jobId,
      @RequestParam String policyType) {

    log.info("[Processing API] POST /trigger - jobId={}, policyType={}", jobId, policyType);

    // Fetch job details from Ingestion
    IngestionJob job = ingestionService.getJob(jobId);

    processingService.processFile(jobId, job.getFilePath(), job.getInsurerId(), policyType);
    
    return ResponseEntity.ok("Processing started for jobId: " + jobId);
  }

  /**
   * Health check endpoint.
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Processing module healthy");
  }
}
