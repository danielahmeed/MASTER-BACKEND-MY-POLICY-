package com.mypolicy.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PolicyRequest {
  @NotBlank(message = "Customer ID is required")
  private String customerId;

  @NotBlank(message = "Insurer ID is required")
  private String insurerId;

  @NotBlank(message = "Policy number is required")
  private String policyNumber;

  @NotBlank(message = "Policy type is required")
  private String policyType;

  private String planName;

  @NotNull(message = "Premium amount is required")
  @Positive(message = "Premium amount must be positive")
  private BigDecimal premiumAmount;

  @NotNull(message = "Sum assured is required")
  @Positive(message = "Sum assured must be positive")
  private BigDecimal sumAssured;

  private LocalDate startDate;
  private LocalDate endDate;

  @NotBlank(message = "Status is required")
  private String status;

  public String getCustomerId() { return customerId; }
  public void setCustomerId(String customerId) { this.customerId = customerId; }
  public String getInsurerId() { return insurerId; }
  public void setInsurerId(String insurerId) { this.insurerId = insurerId; }
  public String getPolicyNumber() { return policyNumber; }
  public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
  public String getPolicyType() { return policyType; }
  public void setPolicyType(String policyType) { this.policyType = policyType; }
  public String getPlanName() { return planName; }
  public void setPlanName(String planName) { this.planName = planName; }
  public BigDecimal getPremiumAmount() { return premiumAmount; }
  public void setPremiumAmount(BigDecimal premiumAmount) { this.premiumAmount = premiumAmount; }
  public BigDecimal getSumAssured() { return sumAssured; }
  public void setSumAssured(BigDecimal sumAssured) { this.sumAssured = sumAssured; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
