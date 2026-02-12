package com.mypolicy.bff.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
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
}
