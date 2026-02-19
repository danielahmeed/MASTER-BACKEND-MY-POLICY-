package com.mypolicy.pipeline.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * Field Mapping model - defines how insurer fields map to standard fields
 * Part of Metadata Module in consolidated Data Pipeline Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping implements Serializable {
  private String sourceField; // Insurer's field name
  private String targetField; // Our standard field name
  private String dataType; // STRING, DECIMAL, DATE, etc.
  private boolean required; // Is this field mandatory?
  private String transformFunction; // Optional transformation logic
}
