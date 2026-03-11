package com.mypolicy.pipeline.metadata.controller;

import com.mypolicy.pipeline.metadata.config.MetadataConfigLoader;
import com.mypolicy.pipeline.metadata.model.FieldMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Public API to view metadata (field mappings) used for processing.
 * Lets insurers see what mappings apply to their uploads.
 */
@RestController
@RequestMapping("/api/public/v1/metadata")
public class PublicMetadataController {

  private final MetadataConfigLoader metadataConfigLoader;

  public PublicMetadataController(MetadataConfigLoader metadataConfigLoader) {
    this.metadataConfigLoader = metadataConfigLoader;
  }

  /**
   * List all configured insurer IDs.
   * GET /api/public/v1/metadata/insurers
   */
  @GetMapping("/insurers")
  public ResponseEntity<Set<String>> listInsurers() {
    Set<String> insurerIds = metadataConfigLoader.getConfiguredInsurerIds();
    return ResponseEntity.ok(insurerIds);
  }

  /**
   * Get field mappings for an insurer and policy type.
   * Shows: sourceField → targetField, with optional transformFunction.
   *
   * GET /api/public/v1/metadata/mappings?insurerId=HEALTH_INSURER&policyType=HEALTH
   */
  @GetMapping("/mappings")
  public ResponseEntity<Map<String, Object>> getMappings(
      @RequestParam String insurerId,
      @RequestParam(required = false) String policyType) {

    String resolvedPolicyType = policyType != null && !policyType.isBlank()
        ? policyType
        : metadataConfigLoader.resolvePolicyType(insurerId, null);

    List<FieldMapping> mappings = metadataConfigLoader.getMappings(insurerId, resolvedPolicyType);

    Map<String, Object> response = new HashMap<>();
    response.put("insurerId", insurerId);
    response.put("policyType", resolvedPolicyType);
    response.put("fieldMappings", mappings);
    response.put("description", "Source CSV/Excel column names map to canonical fields. transformFunction applies normalization (e.g. normalizeDate, normalizeCurrency).");

    return ResponseEntity.ok(response);
  }
}
