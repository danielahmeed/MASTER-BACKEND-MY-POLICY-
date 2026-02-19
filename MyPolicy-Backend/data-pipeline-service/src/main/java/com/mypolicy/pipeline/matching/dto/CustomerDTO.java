package com.mypolicy.pipeline.matching.dto;

import lombok.Data;

/**
 * Customer DTO for communication with Customer Service (external, port 8081).
 */
@Data
public class CustomerDTO {
  private String customerId;
  private String firstName;
  private String lastName;
  private String email;
  private String mobileNumber;
  private String panNumber;
  private String dateOfBirth;
}
