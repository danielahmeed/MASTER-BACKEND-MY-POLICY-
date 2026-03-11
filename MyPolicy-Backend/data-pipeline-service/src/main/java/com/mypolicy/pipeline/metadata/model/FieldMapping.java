package com.mypolicy.pipeline.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * Field Mapping model - defines how insurer fields map to standard fields
 * Part of Metadata Module in consolidated Data Pipeline Service
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping implements Serializable {
  private String sourceField; // Insurer's field name
  private String targetField; // Our standard field name
  private String dataType; // STRING, DECIMAL, DATE, etc.
  private boolean required; // Is this field mandatory?
  private String transformFunction; // Optional transformation logic

  public String getSourceField() { return sourceField; }
  public void setSourceField(String sourceField) { this.sourceField = sourceField; }
  public String getTargetField() { return targetField; }
  public void setTargetField(String targetField) { this.targetField = targetField; }
  public String getDataType() { return dataType; }
  public void setDataType(String dataType) { this.dataType = dataType; }
  public boolean isRequired() { return required; }
  public void setRequired(boolean required) { this.required = required; }
  public String getTransformFunction() { return transformFunction; }
  public void setTransformFunction(String transformFunction) { this.transformFunction = transformFunction; }
}
