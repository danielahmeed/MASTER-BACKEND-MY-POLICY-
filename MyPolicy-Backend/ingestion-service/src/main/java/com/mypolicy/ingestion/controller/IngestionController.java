package com.mypolicy.ingestion.controller;

import com.mypolicy.ingestion.dto.JobStatusResponse;
import com.mypolicy.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

  private final IngestionService ingestionService;

  @PostMapping("/upload")
  public ResponseEntity<String> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam("insurerId") String insurerId,
      @RequestParam("policyType") String policyType) {

    // This endpoint maps to Sequence 5: Manual File Upload
    try {
      String jobId = ingestionService.submitJob(file, insurerId, policyType);
      return ResponseEntity.status(HttpStatus.ACCEPTED).body("Job submitted successfully. Job ID: " + jobId);
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.");
    }
  }

  @GetMapping("/status/{jobId}")
  public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
    return ResponseEntity.ok(ingestionService.getJobStatus(jobId));
  }
}
