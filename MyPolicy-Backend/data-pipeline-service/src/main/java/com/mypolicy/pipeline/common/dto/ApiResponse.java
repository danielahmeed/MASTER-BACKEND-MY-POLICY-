package com.mypolicy.pipeline.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API Response Wrapper for consistent response structure across all
 * endpoints.
 * 
 * @param <T> The type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  /**
   * Indicates if the request was successful
   */
  private boolean success;

  /**
   * Human-readable message about the operation
   */
  private String message;

  /**
   * The actual response data
   */
  private T data;

  /**
   * Error details (only present if success is false)
   */
  private ErrorDetails error;

  /**
   * Timestamp of the response
   */
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  /**
   * Factory method for successful response with data
   */
  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .message("Request processed successfully")
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Factory method for successful response with data and custom message
   */
  public static <T> ApiResponse<T> success(T data, String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Factory method for successful response without data
   */
  public static <T> ApiResponse<T> success(String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Factory method for error response
   */
  public static <T> ApiResponse<T> error(String message, String errorCode) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .error(ErrorDetails.builder()
            .code(errorCode)
            .message(message)
            .build())
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Factory method for error response with details
   */
  public static <T> ApiResponse<T> error(String message, String errorCode, String details) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .error(ErrorDetails.builder()
            .code(errorCode)
            .message(message)
            .details(details)
            .build())
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Error details structure
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ErrorDetails {
    private String code;
    private String message;
    private String details;
  }
}
