package com.mypolicy.policy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request for correcting/updating policy data.
 * All fields optional except reason (for audit).
 */
@Data
public class PolicyCorrectionRequest {

  private String customerId;
  private String planName;
  private BigDecimal premiumAmount;
  private BigDecimal sumAssured;
  private LocalDate startDate;
  private LocalDate endDate;

  @NotBlank(message = "reason is required for audit compliance")
  private String reason;
}
