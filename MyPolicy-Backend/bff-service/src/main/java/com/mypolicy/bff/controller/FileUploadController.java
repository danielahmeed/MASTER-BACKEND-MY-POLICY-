package com.mypolicy.bff.controller;

import com.mypolicy.bff.client.IngestionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bff/upload")
@RequiredArgsConstructor
public class FileUploadController {

  private final IngestionClient ingestionClient;

  @PostMapping
  public ResponseEntity<Object> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam("uploadedBy") String uploadedBy,
      @RequestParam("insurerId") String insurerId) {

    return ResponseEntity.ok(ingestionClient.uploadFile(file, insurerId, uploadedBy));
  }

  @GetMapping("/status/{jobId}")
  public ResponseEntity<Object> getJobStatus(@PathVariable String jobId) {
    return ResponseEntity.ok(ingestionClient.getJobStatus(jobId));
  }
}
