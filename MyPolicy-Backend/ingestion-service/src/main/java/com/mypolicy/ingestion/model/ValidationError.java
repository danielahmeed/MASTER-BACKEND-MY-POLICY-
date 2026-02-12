package com.mypolicy.ingestion.model;

import lombok.Data;

@Data
public class ValidationError {
  private int rowNumber;
  private String field;
  private String errorMessage;
  private String value;
}
