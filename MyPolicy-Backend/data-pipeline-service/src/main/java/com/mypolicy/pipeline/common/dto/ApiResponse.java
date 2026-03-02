package com.mypolicy.pipeline.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API Response Wrapper for consistent response structure across all
 * endpoints.
 * 
 * @param <T> The type of data being returned
 */
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
  private LocalDateTime timestamp = LocalDateTime.now();

  public boolean isSuccess() { return success; }
  public void setSuccess(boolean success) { this.success = success; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public T getData() { return data; }
  public void setData(T data) { this.data = data; }
  public ErrorDetails getError() { return error; }
  public void setError(ErrorDetails error) { this.error = error; }
  public LocalDateTime getTimestamp() { return timestamp; }
  public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

  public static <T> ApiResponse<T> success(T data) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = true;
    r.message = "Request processed successfully";
    r.data = data;
    r.timestamp = LocalDateTime.now();
    return r;
  }

  public static <T> ApiResponse<T> success(T data, String message) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = true;
    r.message = message;
    r.data = data;
    r.timestamp = LocalDateTime.now();
    return r;
  }

  public static <T> ApiResponse<T> success(String message) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = true;
    r.message = message;
    r.timestamp = LocalDateTime.now();
    return r;
  }

  public static <T> ApiResponse<T> error(String message, String errorCode) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = false;
    r.message = message;
    r.error = new ErrorDetails(errorCode, message, null);
    r.timestamp = LocalDateTime.now();
    return r;
  }

  public static <T> ApiResponse<T> error(String message, String errorCode, String details) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = false;
    r.message = message;
    r.error = new ErrorDetails(errorCode, message, details);
    r.timestamp = LocalDateTime.now();
    return r;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ErrorDetails {
    private String code;
    private String message;
    private String details;

    public ErrorDetails() {}
    public ErrorDetails(String code, String message, String details) {
      this.code = code;
      this.message = message;
      this.details = details;
    }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
  }
}
