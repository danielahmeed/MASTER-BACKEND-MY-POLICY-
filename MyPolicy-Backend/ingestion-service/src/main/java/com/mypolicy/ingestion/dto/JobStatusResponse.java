package com.mypolicy.ingestion.dto;

import com.mypolicy.ingestion.model.IngestionStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class JobStatusResponse {
  private String jobId;
  private String fileName;
  private IngestionStatus status;
  private int processed;
  private int total;
  private LocalDateTime uploadedAt;
}
