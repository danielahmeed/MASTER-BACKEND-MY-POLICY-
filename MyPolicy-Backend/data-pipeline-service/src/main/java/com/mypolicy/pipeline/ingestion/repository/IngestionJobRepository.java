package com.mypolicy.pipeline.ingestion.repository;

import com.mypolicy.pipeline.ingestion.model.IngestionJob;
import com.mypolicy.pipeline.ingestion.model.IngestionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for ingestion job tracking.
 * 
 * Consolidated Service: Part of data-pipeline-service.
 */
@Repository
public interface IngestionJobRepository extends MongoRepository<IngestionJob, String> {
  List<IngestionJob> findByStatus(IngestionStatus status);

  List<IngestionJob> findByInsurerId(String insurerId);
}
