package com.mypolicy.ingestion.model;

/**
 * Represents a single validation error (header or row-level).
 */
public class ValidationError {
  private int rowNumber;
  private String field;
  private String errorMessage;
  private String value;

  public int getRowNumber() { return rowNumber; }
  public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
  public String getField() { return field; }
  public void setField(String field) { this.field = field; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
  public String getValue() { return value; }
  public void setValue(String value) { this.value = value; }
}
