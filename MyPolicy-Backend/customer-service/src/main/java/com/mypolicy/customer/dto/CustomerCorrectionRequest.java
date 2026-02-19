package com.mypolicy.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request for correcting/updating customer data.
 * All fields optional except reason (for audit).
 */
@Data
public class CustomerCorrectionRequest {

  private String firstName;
  private String lastName;
  private String mobileNumber;
  private String email;
  private String address;
  private String panNumber;
  private LocalDate dateOfBirth;

  @NotBlank(message = "reason is required for audit compliance")
  private String reason;
}
