package com.mypolicy.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping implements Serializable {
  private String sourceField;
  private String targetField;
  private String dataType;
  private boolean required;
  private String transformFunction;
}
