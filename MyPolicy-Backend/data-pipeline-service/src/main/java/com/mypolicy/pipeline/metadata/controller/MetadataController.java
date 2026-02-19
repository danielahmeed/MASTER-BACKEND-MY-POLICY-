package com.mypolicy.pipeline.metadata.controller;

import com.mypolicy.pipeline.metadata.model.FieldMapping;
import com.mypolicy.pipeline.metadata.model.InsurerConfiguration;
import com.mypolicy.pipeline.metadata.service.MetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Metadata Controller - REST endpoints for insurer configuration
 * Part of Metadata Module in consolidated Data Pipeline Service
 * 
 * Endpoints remain the same:
 * - POST /api/v1/metadata/config
 * - GET /api/v1/metadata/config/{insurerId}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
public class MetadataController {

  private final MetadataService metadataService;

  /**
   * Create or update insurer configuration
   * 
   * Example:
   * POST /api/v1/metadata/config?insurerId=HDFC_LIFE&insurerName=HDFC Life
   * Body: { "TERM_LIFE": [ {...field mappings...} ] }
   */
  @PostMapping("/config")
  public ResponseEntity<InsurerConfiguration> createConfiguration(
      @RequestParam String insurerId,
      @RequestParam String insurerName,
      @RequestBody Map<String, List<FieldMapping>> mappings) {

    log.info("Received configuration request for insurer: {}", insurerId);

    InsurerConfiguration config = metadataService.saveConfiguration(
        insurerId, insurerName, mappings);

    return ResponseEntity.ok(config);
  }

  /**
   * Get configuration for an insurer
   * 
   * Example:
   * GET /api/v1/metadata/config/HDFC_LIFE
   */
  @GetMapping("/config/{insurerId}")
  public ResponseEntity<InsurerConfiguration> getConfiguration(
      @PathVariable String insurerId) {

    log.info("Fetching configuration for insurer: {}", insurerId);

    InsurerConfiguration config = metadataService.getConfiguration(insurerId);

    return ResponseEntity.ok(config);
  }

  /**
   * Health check endpoint
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Metadata Module: OK");
  }
}
