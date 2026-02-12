package com.mypolicy.customer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String customerId;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String mobileNumber;

  @Column(unique = true)
  private String panNumber;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(nullable = false)
  private String passwordHash;

  @Column(columnDefinition = "TEXT")
  private String address;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private CustomerStatus status = CustomerStatus.ACTIVE;

  @Column(updatable = false)
  private LocalDateTime createdAt;

  @Column
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
