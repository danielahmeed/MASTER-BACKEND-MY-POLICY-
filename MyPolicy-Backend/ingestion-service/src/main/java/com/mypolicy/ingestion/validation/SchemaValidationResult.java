package com.mypolicy.ingestion.validation;

import com.mypolicy.ingestion.model.ValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of schema validation. Contains list of validation errors.
 */
public class SchemaValidationResult {

  private final List<ValidationError> errors = new ArrayList<>();

  public void addError(int rowNumber, String field, String errorMessage, String value) {
    ValidationError err = new ValidationError();
    err.setRowNumber(rowNumber);
    err.setField(field);
    err.setErrorMessage(errorMessage);
    err.setValue(value != null ? value : "");
    errors.add(err);
  }

  public void addHeaderError(String field, String errorMessage) {
    addError(0, field, errorMessage, null);
  }

  public boolean isValid() {
    return errors.isEmpty();
  }

  public List<ValidationError> getErrors() {
    return List.copyOf(errors);
  }

  /**
   * Returns a human-readable error summary for API response.
   */
  public String getErrorSummary() {
    if (errors.isEmpty()) return "";
    StringBuilder sb = new StringBuilder("Schema validation failed: ");
    for (int i = 0; i < Math.min(errors.size(), 5); i++) {
      ValidationError e = errors.get(i);
      if (e.getRowNumber() == 0) {
        sb.append(e.getErrorMessage());
      } else {
        sb.append("Row ").append(e.getRowNumber()).append(" ").append(e.getField())
            .append(": ").append(e.getErrorMessage());
      }
      if (i < Math.min(errors.size(), 5) - 1) sb.append("; ");
    }
    if (errors.size() > 5) sb.append(" (+").append(errors.size() - 5).append(" more)");
    return sb.toString();
  }
}
