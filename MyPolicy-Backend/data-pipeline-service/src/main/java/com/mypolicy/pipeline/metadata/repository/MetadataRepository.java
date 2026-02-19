package com.mypolicy.pipeline.metadata.repository;

import com.mypolicy.pipeline.metadata.model.InsurerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Insurer Configuration
 * Part of Metadata Module in consolidated Data Pipeline Service
 */
@Repository
public interface MetadataRepository extends JpaRepository<InsurerConfiguration, String> {
  Optional<InsurerConfiguration> findByInsurerId(String insurerId);
}
