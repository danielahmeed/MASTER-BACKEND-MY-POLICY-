package com.mypolicy.bff.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PortfolioResponse {
  private CustomerDTO customer;
  private List<PolicyDTO> policies;
  private int totalPolicies;
  private BigDecimal totalPremium;
  private BigDecimal totalCoverage;
}
