package com.mypolicy.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageInsights {

  // Customer context
  private String customerId;
  private String customerName;

  // Current coverage summary
  private Map<String, CoverageByType> coverageByType;
  private BigDecimal totalCoverage;
  private BigDecimal totalPremium;

  // Gap analysis
  private List<CoverageGap> gaps;

  // Recommendations
  private List<Recommendation> recommendations;

  // Overall score
  private CoverageScore overallScore;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CoverageByType {
    private String policyType;
    private int policyCount;
    private BigDecimal totalCoverage;
    private BigDecimal totalPremium;
    private BigDecimal recommendedCoverage;
    private boolean adequate;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CoverageGap {
    private String policyType;
    private BigDecimal currentCoverage;
    private BigDecimal recommendedCoverage;
    private BigDecimal gap;
    private String severity; // HIGH, MEDIUM, LOW
    private String advisory;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Recommendation {
    private String policyType;
    private String title;
    private String description;
    private BigDecimal suggestedCoverage;
    private BigDecimal estimatedPremium;
    private String priority; // CRITICAL, HIGH, MEDIUM, LOW
    private String rationale;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CoverageScore {
    private int score; // 0-100
    private String rating; // EXCELLENT, GOOD, FAIR, POOR
    private String summary;
  }
}
