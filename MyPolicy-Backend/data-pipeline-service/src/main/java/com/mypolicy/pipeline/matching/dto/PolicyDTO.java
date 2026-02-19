package com.mypolicy.pipeline.matching.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Policy DTO for communication with Policy Service (external, port 8085).
 */
@Data
public class PolicyDTO {
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
