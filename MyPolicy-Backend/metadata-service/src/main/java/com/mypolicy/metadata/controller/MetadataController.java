package com.mypolicy.metadata.controller;

import com.mypolicy.metadata.model.FieldMapping;
import com.mypolicy.metadata.model.InsurerConfiguration;
import com.mypolicy.metadata.service.MetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
public class MetadataController {

  private final MetadataService metadataService;

  @PostMapping("/config")
  public ResponseEntity<InsurerConfiguration> createConfiguration(
      @RequestParam String insurerId,
      @RequestParam String insurerName,
      @RequestBody Map<String, List<FieldMapping>> mappings) {

    return ResponseEntity.ok(
        metadataService.saveConfiguration(insurerId, insurerName, mappings));
  }

  @GetMapping("/config/{insurerId}")
  public ResponseEntity<InsurerConfiguration> getConfiguration(@PathVariable String insurerId) {
    return ResponseEntity.ok(metadataService.getConfiguration(insurerId));
  }
}
