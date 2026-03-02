package com.mypolicy.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for bulk customer import (e.g. from CSV). Password uses default if not provided.
 */
@Data
public class CustomerBulkCreateRequest {
  @NotBlank(message = "First name is required")
  private String firstName;

  @NotBlank(message = "Last name is required")
  private String lastName;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Mobile number is required")
  private String mobileNumber;

  private String panNumber;
  private LocalDate dateOfBirth;
  private String address;
  private String city;
  /** If null, a default password is used for bulk-imported customers. */
  private String password;
}
