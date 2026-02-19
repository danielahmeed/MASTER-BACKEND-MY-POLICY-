package com.mypolicy.pipeline.metadata.service;

import com.mypolicy.pipeline.metadata.model.FieldMapping;
import com.mypolicy.pipeline.metadata.model.InsurerConfiguration;
import com.mypolicy.pipeline.metadata.repository.MetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Metadata Service - Manages insurer configurations and field mappings
 * Part of Metadata Module in consolidated Data Pipeline Service
 * 
 * NOTE: This is now a service layer class, not a separate microservice!
 * No HTTP calls - just method calls within the same JVM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {

  private final MetadataRepository repository;

  /**
   * Save or update insurer configuration
   * Called by admin during setup phase
   */
  public InsurerConfiguration saveConfiguration(String insurerId, String insurerName,
      Map<String, List<FieldMapping>> mappings) {

    log.info("Saving configuration for insurer: {}", insurerId);

    Optional<InsurerConfiguration> existing = repository.findByInsurerId(insurerId);

    InsurerConfiguration config = existing.orElse(new InsurerConfiguration());
    config.setInsurerId(insurerId);
    config.setInsurerName(insurerName);
    config.setFieldMappings(mappings);
    config.setActive(true);
    config.setUpdatedAt(LocalDateTime.now());

    InsurerConfiguration saved = repository.save(config);
    log.info("Configuration saved with ID: {}", saved.getConfigId());

    return saved;
  }

  /**
   * Get configuration for an insurer
   * Called by Processing Module (no HTTP call, just method call!)
   */
  @Cacheable(value = "insurerConfigs", key = "#insurerId")
  public InsurerConfiguration getConfiguration(String insurerId) {
    log.debug("Fetching configuration for insurer: {}", insurerId);

    return repository.findByInsurerId(insurerId)
        .orElseThrow(() -> new RuntimeException("Configuration not found for: " + insurerId));
  }

  /**
   * Get field mappings for a specific policy type
   * Helper method for Processing Module
   */
  public List<FieldMapping> getMappingsForPolicyType(String insurerId, String policyType) {
    InsurerConfiguration config = getConfiguration(insurerId);

    List<FieldMapping> mappings = config.getFieldMappings().get(policyType);

    if (mappings == null) {
      throw new RuntimeException("No mappings found for " + insurerId + " - " + policyType);
    }

    return mappings;
  }
}
