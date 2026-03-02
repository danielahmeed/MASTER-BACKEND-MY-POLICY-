package com.mypolicy.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerBulkCreateResponse {
  private int totalRequested;
  private int created;
  private int skipped; // e.g. already existed
  private List<String> createdCustomerIds;
  private List<CreatedCustomerInfo> createdCustomers; // customerId + email for matching
  private List<String> errors; // e.g. "Row 3: Email already exists"

  @Data
  @Builder
  public static class CreatedCustomerInfo {
    private String customerId;
    private String email;
  }
}
