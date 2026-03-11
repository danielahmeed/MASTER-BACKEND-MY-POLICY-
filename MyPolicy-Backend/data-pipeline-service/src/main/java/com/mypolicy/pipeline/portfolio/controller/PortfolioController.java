package com.mypolicy.pipeline.portfolio.controller;

import com.mypolicy.pipeline.portfolio.model.CustomerPortfolio;
import com.mypolicy.pipeline.portfolio.service.CustomerPortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * API to view consolidated customer + policies (MongoDB customer_portfolios).
 */
@RestController
@RequestMapping("/api/public/v1/portfolio")
public class PortfolioController {

  private final CustomerPortfolioService portfolioService;

  public PortfolioController(CustomerPortfolioService portfolioService) {
    this.portfolioService = portfolioService;
  }

  /**
   * List all customer portfolios (customer + all their insurance policies).
   * GET /api/public/v1/portfolio
   */
  @GetMapping
  public ResponseEntity<List<CustomerPortfolio>> listAll() {
    return ResponseEntity.ok(portfolioService.findAll());
  }

  /**
   * Get portfolio for a specific customer.
   * GET /api/public/v1/portfolio/{customerId}
   */
  @GetMapping("/{customerId}")
  public ResponseEntity<?> getByCustomerId(@PathVariable String customerId) {
    Optional<CustomerPortfolio> opt = portfolioService.findByCustomerId(customerId);
    return opt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }
}
