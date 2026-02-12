package com.mypolicy.bff.controller;

import com.mypolicy.bff.dto.CoverageInsights;
import com.mypolicy.bff.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/insights")
@RequiredArgsConstructor
public class InsightsController {

  private final InsightsService insightsService;

  /**
   * Get comprehensive coverage insights and recommendations
   */
  @GetMapping("/{customerId}")
  public ResponseEntity<CoverageInsights> getCoverageInsights(@PathVariable String customerId) {
    return ResponseEntity.ok(insightsService.analyzeCoverage(customerId));
  }
}
