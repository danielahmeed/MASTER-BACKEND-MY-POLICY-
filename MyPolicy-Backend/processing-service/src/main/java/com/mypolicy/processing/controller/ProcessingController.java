package com.mypolicy.processing.controller;

import com.mypolicy.processing.service.ExcelProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/processing")
@RequiredArgsConstructor
public class ProcessingController {

  private final ExcelProcessingService processingService;

  // TODO: This should eventually be replaced/augmented by a Kafka Consumer
  @PostMapping("/trigger")
  public ResponseEntity<String> triggerProcessing(
      @RequestParam String filePath,
      @RequestParam String insurerId,
      @RequestParam String policyType) {

    processingService.processFile(filePath, insurerId, policyType);
    return ResponseEntity.ok("Processing started for file: " + filePath);
  }
}
