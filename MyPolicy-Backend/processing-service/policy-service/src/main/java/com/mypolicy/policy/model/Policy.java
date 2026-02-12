package com.mypolicy.policy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "policies")
public class Policy {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String customerId; // Linked to Customer Service

  @Column(nullable = false)
  private String insurerId; // Linked to Metadata Service rules

  @Column(nullable = false, unique = true)
  private String policyNumber;

  @Column(nullable = false)
  private String policyType; // e.g., TERM_LIFE, HEALTH

  private String planName;

  @Column(nullable = false)
  private BigDecimal premiumAmount;

  @Column(nullable = false)
  private BigDecimal sumAssured;

  private LocalDate startDate;
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  private PolicyStatus status;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
