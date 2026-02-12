package com.mypolicy.bff.service;

import com.mypolicy.bff.client.CustomerClient;
import com.mypolicy.bff.client.PolicyClient;
import com.mypolicy.bff.dto.CoverageInsights;
import com.mypolicy.bff.dto.CustomerDTO;
import com.mypolicy.bff.dto.PolicyDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightsService {

  private final CustomerClient customerClient;
  private final PolicyClient policyClient;

  // Recommended coverage multipliers (based on industry standards)
  private static final Map<String, BigDecimal> RECOMMENDED_COVERAGE = Map.of(
      "TERM_LIFE", new BigDecimal("10000000"), // ₹1 Crore base
      "HEALTH", new BigDecimal("1000000"), // ₹10 Lakhs base
      "MOTOR", new BigDecimal("500000"), // ₹5 Lakhs base
      "HOME", new BigDecimal("2000000"), // ₹20 Lakhs base
      "TRAVEL", new BigDecimal("200000") // ₹2 Lakhs base
  );

  /**
   * Generate comprehensive coverage insights for a customer
   */
  public CoverageInsights analyzeCoverage(String customerId) {
    log.info("Analyzing coverage for customer: {}", customerId);

    // Fetch customer and policies
    CustomerDTO customer = customerClient.getCustomerById(customerId);
    List<PolicyDTO> policies = policyClient.getPoliciesByCustomer(customerId);

    // Group policies by type
    Map<String, List<PolicyDTO>> policiesByType = policies.stream()
        .collect(Collectors.groupingBy(PolicyDTO::getPolicyType));

    // Calculate coverage by type
    Map<String, CoverageInsights.CoverageByType> coverageByType = calculateCoverageByType(policiesByType);

    // Identify gaps
    List<CoverageInsights.CoverageGap> gaps = identifyGaps(coverageByType);

    // Generate recommendations
    List<CoverageInsights.Recommendation> recommendations = generateRecommendations(gaps, policiesByType);

    // Calculate overall score
    CoverageInsights.CoverageScore score = calculateCoverageScore(coverageByType, gaps);

    // Calculate totals
    BigDecimal totalCoverage = policies.stream()
        .map(PolicyDTO::getSumAssured)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalPremium = policies.stream()
        .map(PolicyDTO::getPremiumAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return CoverageInsights.builder()
        .customerId(customerId)
        .customerName(customer.getFirstName() + " " + customer.getLastName())
        .coverageByType(coverageByType)
        .totalCoverage(totalCoverage)
        .totalPremium(totalPremium)
        .gaps(gaps)
        .recommendations(recommendations)
        .overallScore(score)
        .build();
  }

  /**
   * Calculate coverage breakdown by policy type
   */
  private Map<String, CoverageInsights.CoverageByType> calculateCoverageByType(
      Map<String, List<PolicyDTO>> policiesByType) {

    Map<String, CoverageInsights.CoverageByType> result = new HashMap<>();

    for (Map.Entry<String, List<PolicyDTO>> entry : policiesByType.entrySet()) {
      String type = entry.getKey();
      List<PolicyDTO> typePolicies = entry.getValue();

      BigDecimal totalCoverage = typePolicies.stream()
          .map(PolicyDTO::getSumAssured)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal totalPremium = typePolicies.stream()
          .map(PolicyDTO::getPremiumAmount)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal recommended = RECOMMENDED_COVERAGE.getOrDefault(type, BigDecimal.ZERO);
      boolean adequate = totalCoverage.compareTo(recommended) >= 0;

      result.put(type, CoverageInsights.CoverageByType.builder()
          .policyType(type)
          .policyCount(typePolicies.size())
          .totalCoverage(totalCoverage)
          .totalPremium(totalPremium)
          .recommendedCoverage(recommended)
          .adequate(adequate)
          .build());
    }

    return result;
  }

  /**
   * Identify coverage gaps
   */
  private List<CoverageInsights.CoverageGap> identifyGaps(
      Map<String, CoverageInsights.CoverageByType> coverageByType) {

    List<CoverageInsights.CoverageGap> gaps = new ArrayList<>();

    // Check each recommended policy type
    for (Map.Entry<String, BigDecimal> entry : RECOMMENDED_COVERAGE.entrySet()) {
      String type = entry.getKey();
      BigDecimal recommended = entry.getValue();

      CoverageInsights.CoverageByType current = coverageByType.get(type);
      BigDecimal currentCoverage = current != null ? current.getTotalCoverage() : BigDecimal.ZERO;

      if (currentCoverage.compareTo(recommended) < 0) {
        BigDecimal gap = recommended.subtract(currentCoverage);
        String severity = calculateSeverity(currentCoverage, recommended);
        String advisory = generateAdvisory(type, currentCoverage, recommended, gap);

        gaps.add(CoverageInsights.CoverageGap.builder()
            .policyType(type)
            .currentCoverage(currentCoverage)
            .recommendedCoverage(recommended)
            .gap(gap)
            .severity(severity)
            .advisory(advisory)
            .build());
      }
    }

    return gaps;
  }

  /**
   * Generate recommendations based on gaps
   */
  private List<CoverageInsights.Recommendation> generateRecommendations(
      List<CoverageInsights.CoverageGap> gaps,
      Map<String, List<PolicyDTO>> existingPolicies) {

    List<CoverageInsights.Recommendation> recommendations = new ArrayList<>();

    for (CoverageInsights.CoverageGap gap : gaps) {
      String priority = gap.getSeverity().equals("HIGH") ? "CRITICAL"
          : gap.getSeverity().equals("MEDIUM") ? "HIGH" : "MEDIUM";

      BigDecimal estimatedPremium = estimatePremium(gap.getPolicyType(), gap.getGap());
      String rationale = generateRationale(gap.getPolicyType(), gap.getCurrentCoverage(), gap.getGap());

      recommendations.add(CoverageInsights.Recommendation.builder()
          .policyType(gap.getPolicyType())
          .title(getRecommendationTitle(gap.getPolicyType()))
          .description(getRecommendationDescription(gap.getPolicyType(), gap.getGap()))
          .suggestedCoverage(gap.getRecommendedCoverage())
          .estimatedPremium(estimatedPremium)
          .priority(priority)
          .rationale(rationale)
          .build());
    }

    return recommendations.stream()
        .sorted(Comparator.comparing(r -> getPriorityOrder(r.getPriority())))
        .collect(Collectors.toList());
  }

  /**
   * Calculate overall coverage score (0-100)
   */
  private CoverageInsights.CoverageScore calculateCoverageScore(
      Map<String, CoverageInsights.CoverageByType> coverageByType,
      List<CoverageInsights.CoverageGap> gaps) {

    int totalTypes = RECOMMENDED_COVERAGE.size();
    int adequateTypes = (int) coverageByType.values().stream()
        .filter(CoverageInsights.CoverageByType::isAdequate)
        .count();

    int score = (adequateTypes * 100) / totalTypes;

    String rating = score >= 80 ? "EXCELLENT" : score >= 60 ? "GOOD" : score >= 40 ? "FAIR" : "POOR";

    String summary = generateScoreSummary(score, adequateTypes, totalTypes, gaps.size());

    return CoverageInsights.CoverageScore.builder()
        .score(score)
        .rating(rating)
        .summary(summary)
        .build();
  }

  // Helper methods

  private String calculateSeverity(BigDecimal current, BigDecimal recommended) {
    if (current.compareTo(BigDecimal.ZERO) == 0)
      return "HIGH";

    BigDecimal percentage = current.multiply(new BigDecimal("100"))
        .divide(recommended, 2, RoundingMode.HALF_UP);

    if (percentage.compareTo(new BigDecimal("50")) < 0)
      return "HIGH";
    if (percentage.compareTo(new BigDecimal("75")) < 0)
      return "MEDIUM";
    return "LOW";
  }

  private String generateAdvisory(String type, BigDecimal current, BigDecimal recommended, BigDecimal gap) {
    if (current.compareTo(BigDecimal.ZERO) == 0) {
      return String.format(
          "You don't have any %s coverage. We recommend ₹%s coverage to protect yourself and your family.",
          formatPolicyType(type), formatAmount(recommended));
    }

    return String.format("Your current %s coverage of ₹%s is below the recommended ₹%s. Consider increasing by ₹%s.",
        formatPolicyType(type), formatAmount(current), formatAmount(recommended), formatAmount(gap));
  }

  private String generateRationale(String type, BigDecimal current, BigDecimal gap) {
    switch (type) {
      case "TERM_LIFE":
        return "Life insurance should cover 10-15 times your annual income to ensure your family's financial security.";
      case "HEALTH":
        return "Medical inflation is rising at 15% annually. Adequate health coverage protects you from unexpected medical expenses.";
      case "MOTOR":
        return "Comprehensive motor insurance protects your vehicle and provides third-party liability coverage as mandated by law.";
      case "HOME":
        return "Home insurance protects your most valuable asset against natural disasters, theft, and other unforeseen events.";
      default:
        return "Adequate coverage ensures financial protection against unforeseen circumstances.";
    }
  }

  private String getRecommendationTitle(String type) {
    switch (type) {
      case "TERM_LIFE":
        return "Increase Life Insurance Coverage";
      case "HEALTH":
        return "Enhance Health Insurance Protection";
      case "MOTOR":
        return "Add Motor Insurance Coverage";
      case "HOME":
        return "Protect Your Home with Insurance";
      case "TRAVEL":
        return "Get Travel Insurance Coverage";
      default:
        return "Add " + formatPolicyType(type) + " Coverage";
    }
  }

  private String getRecommendationDescription(String type, BigDecimal gap) {
    return String.format("We recommend adding ₹%s in %s coverage to ensure comprehensive protection.",
        formatAmount(gap), formatPolicyType(type));
  }

  private BigDecimal estimatePremium(String type, BigDecimal coverage) {
    // Simplified premium estimation (0.5% - 2% of coverage)
    BigDecimal rate = switch (type) {
      case "TERM_LIFE" -> new BigDecimal("0.005"); // 0.5%
      case "HEALTH" -> new BigDecimal("0.03"); // 3%
      case "MOTOR" -> new BigDecimal("0.02"); // 2%
      case "HOME" -> new BigDecimal("0.01"); // 1%
      default -> new BigDecimal("0.015"); // 1.5%
    };

    return coverage.multiply(rate).setScale(0, RoundingMode.HALF_UP);
  }

  private String generateScoreSummary(int score, int adequate, int total, int gapCount) {
    if (score >= 80) {
      return String.format("Excellent! You have adequate coverage in %d out of %d key areas.", adequate, total);
    } else if (score >= 60) {
      return String.format("Good coverage, but you have %d gap(s) to address for comprehensive protection.", gapCount);
    } else if (score >= 40) {
      return String.format("Fair coverage. We recommend addressing %d coverage gap(s) to improve your protection.",
          gapCount);
    } else {
      return String.format(
          "Your coverage needs improvement. You have %d critical gap(s) that require immediate attention.", gapCount);
    }
  }

  private String formatPolicyType(String type) {
    return type.replace("_", " ").toLowerCase();
  }

  private String formatAmount(BigDecimal amount) {
    if (amount.compareTo(new BigDecimal("10000000")) >= 0) {
      return amount.divide(new BigDecimal("10000000"), 2, RoundingMode.HALF_UP) + " Cr";
    } else if (amount.compareTo(new BigDecimal("100000")) >= 0) {
      return amount.divide(new BigDecimal("100000"), 2, RoundingMode.HALF_UP) + " L";
    }
    return amount.toString();
  }

  private int getPriorityOrder(String priority) {
    return switch (priority) {
      case "CRITICAL" -> 1;
      case "HIGH" -> 2;
      case "MEDIUM" -> 3;
      case "LOW" -> 4;
      default -> 5;
    };
  }
}
