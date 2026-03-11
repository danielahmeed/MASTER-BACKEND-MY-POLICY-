package com.mypolicy.pipeline.portfolio.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated view: customer details + all their insurance policies.
 * Stored in H2 (JPA) for compatibility without MongoDB.
 */
@Entity
@Table(name = "customer_portfolios")
public class CustomerPortfolio {

  @Id
  @Column(name = "customer_id", nullable = false, unique = true, length = 64)
  private String customerId;

  @Column(name = "first_name", length = 128)
  private String firstName;

  @Column(name = "last_name", length = 128)
  private String lastName;

  @Column(name = "email", length = 256)
  private String email;

  @Column(name = "mobile_number", length = 32)
  private String mobileNumber;

  @Column(name = "pan_number", length = 32)
  private String panNumber;

  @Column(name = "date_of_birth", length = 32)
  private String dateOfBirth;

  @Column(name = "address", length = 512)
  private String address;

  @Column(name = "city", length = 128)
  private String city;

  @Type(JsonType.class)
  @Column(name = "policies", columnDefinition = "varchar(65535)")
  private List<PolicySnapshot> policies = new ArrayList<>();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public CustomerPortfolio() {
  }

  public String getCustomerId() { return customerId; }
  public void setCustomerId(String customerId) { this.customerId = customerId; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getMobileNumber() { return mobileNumber; }
  public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
  public String getPanNumber() { return panNumber; }
  public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
  public String getDateOfBirth() { return dateOfBirth; }
  public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public List<PolicySnapshot> getPolicies() { return policies; }
  public void setPolicies(List<PolicySnapshot> policies) { this.policies = policies; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  public static class PolicySnapshot {
    private String policyId;
    private String policyNumber;
    private String insurerId;
    private String policyType;
    private String planName;
    private java.math.BigDecimal premiumAmount;
    private java.math.BigDecimal sumAssured;
    private String startDate;
    private String endDate;
    private String status;

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getInsurerId() { return insurerId; }
    public void setInsurerId(String insurerId) { this.insurerId = insurerId; }
    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public java.math.BigDecimal getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(java.math.BigDecimal premiumAmount) { this.premiumAmount = premiumAmount; }
    public java.math.BigDecimal getSumAssured() { return sumAssured; }
    public void setSumAssured(java.math.BigDecimal sumAssured) { this.sumAssured = sumAssured; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
  }
}
