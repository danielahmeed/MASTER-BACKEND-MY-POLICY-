package com.mypolicy.processing.dto;

import lombok.Data;

@Data
public class FieldMappingDTO {
  private String sourceField;
  private String targetField;
  private String dataType;
  private boolean required;
  private String transformFunction;
}
