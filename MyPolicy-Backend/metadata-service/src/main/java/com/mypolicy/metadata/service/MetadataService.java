package com.mypolicy.metadata.service;

import com.mypolicy.metadata.model.FieldMapping;
import com.mypolicy.metadata.model.InsurerConfiguration;
import com.mypolicy.metadata.repository.MetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MetadataService {

  private final MetadataRepository repository;

  public InsurerConfiguration saveConfiguration(String insurerId, String insurerName,
      Map<String, List<FieldMapping>> mappings) {

    Optional<InsurerConfiguration> existing = repository.findByInsurerId(insurerId);

    InsurerConfiguration config = existing.orElse(new InsurerConfiguration());
    config.setInsurerId(insurerId);
    config.setInsurerName(insurerName);
    config.setFieldMappings(mappings);
    config.setActive(true);
    config.setUpdatedAt(LocalDateTime.now());

    return repository.save(config);
  }

  public InsurerConfiguration getConfiguration(String insurerId) {
    return repository.findByInsurerId(insurerId)
        .orElseThrow(() -> new RuntimeException("Configuration not found for: " + insurerId));
  }
}
