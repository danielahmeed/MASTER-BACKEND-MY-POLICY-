package com.mypolicy.bff.controller;

import com.mypolicy.bff.dto.PortfolioResponse;
import com.mypolicy.bff.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

  private final PortfolioService portfolioService;

  @GetMapping("/{customerId}")
  public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable String customerId) {
    return ResponseEntity.ok(portfolioService.getPortfolio(customerId));
  }
}
