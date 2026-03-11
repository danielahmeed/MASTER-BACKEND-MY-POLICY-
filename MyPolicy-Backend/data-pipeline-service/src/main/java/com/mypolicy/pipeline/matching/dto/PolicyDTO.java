package com.mypolicy.pipeline.matching.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Policy DTO for communication with Policy Service (external, port 8085).
 */
public class PolicyDTO {
  private String id;
  private String customerId;
  private String insurerId;
  private String policyNumber;
  private String policyType;
  private String planName;
  private BigDecimal premiumAmount;
  private BigDecimal sumAssured;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
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
