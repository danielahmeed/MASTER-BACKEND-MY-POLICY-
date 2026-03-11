package com.mypolicy.pipeline.ingestion.repository;

import com.mypolicy.pipeline.ingestion.model.IngestionJob;
import com.mypolicy.pipeline.ingestion.model.IngestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for ingestion job tracking.
 *
 * Consolidated Service: Part of data-pipeline-service.
 */
@Repository
public interface IngestionJobRepository extends JpaRepository<IngestionJob, String> {
  List<IngestionJob> findByStatus(IngestionStatus status);

  List<IngestionJob> findByInsurerId(String insurerId);
}
