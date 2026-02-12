package com.mypolicy.ingestion.repository;

import com.mypolicy.ingestion.model.IngestionJob;
import com.mypolicy.ingestion.model.IngestionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface IngestionJobRepository extends MongoRepository<IngestionJob, String> {
  List<IngestionJob> findByStatus(IngestionStatus status);

  List<IngestionJob> findByInsurerId(String insurerId);
}
