package com.mypolicy.pipeline.matching.dto;

/**
 * Customer DTO for communication with Customer Service (external, port 8081).
 */
public class CustomerDTO {
  private String customerId;
  private String firstName;
  private String lastName;
  private String email;
  private String mobileNumber;
  private String panNumber;
  private String dateOfBirth;

  public String getCustomerId() { return customerId; }
  public void setCustomerId(String customerId) { this.customerId = customerId; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getMobileNumber() { return mobileNumber; }
  public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
  public String getPanNumber() { return panNumber; }
  public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
  public String getDateOfBirth() { return dateOfBirth; }
  public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}
