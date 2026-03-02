package com.mypolicy.pipeline.portfolio.repository;

import com.mypolicy.pipeline.portfolio.model.CustomerPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerPortfolioRepository extends JpaRepository<CustomerPortfolio, String> {
  Optional<CustomerPortfolio> findByCustomerId(String customerId);
}
