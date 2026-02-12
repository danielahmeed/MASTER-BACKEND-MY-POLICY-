package com.mypolicy.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CustomerRegistrationRequest {
  @NotBlank(message = "First name is required")
  private String firstName;

  @NotBlank(message = "Last name is required")
  private String lastName;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Mobile number is required")
  private String mobileNumber;

  @NotBlank(message = "Password is required")
  private String password;

  private String panNumber;
  private LocalDate dateOfBirth;
  private String address;
}
