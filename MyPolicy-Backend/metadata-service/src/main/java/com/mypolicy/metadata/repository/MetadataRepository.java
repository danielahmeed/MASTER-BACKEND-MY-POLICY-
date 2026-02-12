package com.mypolicy.metadata.repository;

import com.mypolicy.metadata.model.InsurerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MetadataRepository extends JpaRepository<InsurerConfiguration, String> {
  Optional<InsurerConfiguration> findByInsurerId(String insurerId);
}
