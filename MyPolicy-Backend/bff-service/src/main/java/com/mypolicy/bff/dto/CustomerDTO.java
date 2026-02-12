package com.mypolicy.bff.dto;

import lombok.Data;

@Data
public class CustomerDTO {
  private String customerId;
  private String firstName;
  private String lastName;
  private String email;
  private String mobileNumber;
  private String status;
}
