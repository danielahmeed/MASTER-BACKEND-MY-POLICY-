package com.mypolicy.pipeline.common.exception;

import com.mypolicy.pipeline.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across the
 * application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handle validation errors from request body
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {

    Map<String, String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
            (existing, replacement) -> existing));

    log.warn("Validation failed: {}", errors);

    ApiResponse<Map<String, String>> resp = new ApiResponse<>();
    resp.setSuccess(false);
    resp.setMessage("Validation failed");
    resp.setData(errors);
    ApiResponse.ErrorDetails err = new ApiResponse.ErrorDetails("VALIDATION_ERROR", "One or more fields have validation errors", null);
    resp.setError(err);
    resp.setTimestamp(java.time.LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
  }

  /**
   * Handle IllegalArgumentException (typically validation errors)
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("Invalid argument: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getMessage(), "INVALID_ARGUMENT"));
  }

  /**
   * Handle IllegalStateException (typically state transition errors)
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
    log.warn("Invalid state: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(ex.getMessage(), "INVALID_STATE"));
  }

  /**
   * Handle generic runtime exceptions
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
    log.error("Runtime exception occurred", ex);
    String details = buildDetails(ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(
            "An unexpected error occurred",
            "INTERNAL_ERROR",
            details));
  }

  /**
   * Handle all other exceptions
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
    log.error("Unexpected exception occurred", ex);
    String details = buildDetails(ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(
            "An unexpected error occurred",
            "INTERNAL_ERROR",
            details));
  }

  private String buildDetails(Throwable ex) {
    if (ex.getCause() != null && ex.getCause() != ex) {
      return ex.getMessage() + " | Cause: " + ex.getCause().getMessage();
    }
    return ex.getMessage();
  }
}
