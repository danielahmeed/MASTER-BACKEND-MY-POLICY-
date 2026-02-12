package com.mypolicy.processing.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class InsurerConfigurationDTO {
  private String configId;
  private String insurerId;
  private String insurerName;
  private Map<String, List<FieldMappingDTO>> fieldMappings;
  private boolean active;
}
