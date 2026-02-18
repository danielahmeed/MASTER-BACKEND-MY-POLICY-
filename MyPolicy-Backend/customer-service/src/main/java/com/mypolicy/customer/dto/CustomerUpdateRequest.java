package com.mypolicy.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdateRequest {

  private String firstName;

  private String lastName;

  @Email(message = "Invalid email format")
  private String email;

  @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
  private String mobileNumber;

  @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format")
  private String panNumber;

  private String dateOfBirth;  // Format: YYYY-MM-DD

  private String address;

  // Note: Password update should be handled separately with old password verification
}
