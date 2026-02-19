package com.mypolicy.pipeline.ingestion.model;

/**
 * Ingestion job lifecycle states.
 * State machine: UPLOADED → PROCESSING → COMPLETED | FAILED
 */
public enum IngestionStatus {
  UPLOADED, PROCESSING, COMPLETED, FAILED
}
