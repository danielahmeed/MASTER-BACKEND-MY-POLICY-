package com.mypolicy.matching.dto;

import lombok.Data;

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
