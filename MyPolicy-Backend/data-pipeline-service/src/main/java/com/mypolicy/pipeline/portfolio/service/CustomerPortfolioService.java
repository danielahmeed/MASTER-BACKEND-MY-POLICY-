package com.mypolicy.pipeline.portfolio.service;

import com.mypolicy.pipeline.matching.dto.CustomerDTO;
import com.mypolicy.pipeline.portfolio.model.CustomerPortfolio;
import com.mypolicy.pipeline.portfolio.repository.CustomerPortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Maintains consolidated customer + policies view in MongoDB.
 */
@Service
public class CustomerPortfolioService {

  private static final Logger log = LoggerFactory.getLogger(CustomerPortfolioService.class);

  private final CustomerPortfolioRepository portfolioRepository;

  public CustomerPortfolioService(CustomerPortfolioRepository portfolioRepository) {
    this.portfolioRepository = portfolioRepository;
  }

  /**
   * Upsert portfolio with customer details (e.g. after bulk customer import).
   */
  public void upsertCustomer(CustomerDTO customer, String address, String city) {
    CustomerPortfolio pf = portfolioRepository.findByCustomerId(customer.getCustomerId())
        .orElse(new CustomerPortfolio());
    pf.setCustomerId(customer.getCustomerId());
    pf.setFirstName(customer.getFirstName());
    pf.setLastName(customer.getLastName());
    pf.setEmail(customer.getEmail());
    pf.setMobileNumber(customer.getMobileNumber());
    pf.setPanNumber(customer.getPanNumber());
    pf.setDateOfBirth(customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null);
    pf.setAddress(address);
    pf.setCity(city);
    pf.setUpdatedAt(LocalDateTime.now());
    if (pf.getPolicies() == null) pf.setPolicies(new java.util.ArrayList<>());
    portfolioRepository.save(pf);
    log.debug("[Portfolio] Upserted customer {}", customer.getCustomerId());
  }

  /**
   * Upsert portfolio from raw customer fields (from CSV mapping).
   */
  public void upsertCustomerFromRecord(String customerId, String firstName, String lastName, String email,
      String mobileNumber, String panNumber, String dateOfBirth, String address, String city) {
    CustomerPortfolio pf = portfolioRepository.findByCustomerId(customerId)
        .orElse(new CustomerPortfolio());
    pf.setCustomerId(customerId);
    pf.setFirstName(firstName);
    pf.setLastName(lastName);
    pf.setEmail(email);
    pf.setMobileNumber(mobileNumber);
    pf.setPanNumber(panNumber);
    pf.setDateOfBirth(dateOfBirth);
    pf.setAddress(address);
    pf.setCity(city);
    pf.setUpdatedAt(LocalDateTime.now());
    if (pf.getPolicies() == null) pf.setPolicies(new java.util.ArrayList<>());
    portfolioRepository.save(pf);
    log.debug("[Portfolio] Upserted customer from record {}", customerId);
  }

  /**
   * Add a policy to a customer's portfolio (call after policy creation).
   */
  public void addPolicyToCustomer(String customerId, String policyId, String policyNumber, String insurerId,
      String policyType, String planName, BigDecimal premiumAmount, BigDecimal sumAssured,
      String startDate, String endDate, String status) {
    CustomerPortfolio pf = portfolioRepository.findByCustomerId(customerId)
        .orElseGet(() -> {
          CustomerPortfolio p = new CustomerPortfolio();
          p.setCustomerId(customerId);
          p.setPolicies(new java.util.ArrayList<>());
          return p;
        });
    if (pf.getPolicies() == null) pf.setPolicies(new java.util.ArrayList<>());
    CustomerPortfolio.PolicySnapshot snap = new CustomerPortfolio.PolicySnapshot();
    snap.setPolicyId(policyId);
    snap.setPolicyNumber(policyNumber);
    snap.setInsurerId(insurerId);
    snap.setPolicyType(policyType);
    snap.setPlanName(planName);
    snap.setPremiumAmount(premiumAmount);
    snap.setSumAssured(sumAssured);
    snap.setStartDate(startDate);
    snap.setEndDate(endDate);
    snap.setStatus(status);
    pf.getPolicies().add(snap);
    pf.setUpdatedAt(LocalDateTime.now());
    portfolioRepository.save(pf);
    log.debug("[Portfolio] Added policy {} to customer {}", policyNumber, customerId);
  }

  public Optional<CustomerPortfolio> findByCustomerId(String customerId) {
    return portfolioRepository.findByCustomerId(customerId);
  }

  public List<CustomerPortfolio> findAll() {
    return portfolioRepository.findAll();
  }
}
