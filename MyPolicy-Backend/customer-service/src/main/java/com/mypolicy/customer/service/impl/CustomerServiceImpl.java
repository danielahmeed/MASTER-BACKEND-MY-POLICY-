package com.mypolicy.customer.service.impl;

import com.mypolicy.customer.dto.CustomerCorrectionRequest;
import com.mypolicy.customer.dto.CustomerRegistrationRequest;
import com.mypolicy.customer.dto.CustomerResponse;
import com.mypolicy.customer.dto.CustomerUpdateRequest;
import com.mypolicy.customer.dto.LoginRequest;
import com.mypolicy.customer.model.Customer;
import com.mypolicy.customer.model.CustomerStatus;
import com.mypolicy.customer.repository.CustomerRepository;
import com.mypolicy.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

  private final CustomerRepository customerRepository;
  private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  private final com.mypolicy.customer.security.JwtService jwtService;

  @Override
  @Transactional
  public CustomerResponse registerCustomer(CustomerRegistrationRequest request) {
    if (customerRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already exists");
    }
    if (customerRepository.existsByMobileNumber(request.getMobileNumber())) {
      throw new RuntimeException("Mobile number already exists");
    }

    Customer customer = new Customer();
    customer.setFirstName(request.getFirstName());
    customer.setLastName(request.getLastName());
    customer.setEmail(request.getEmail());
    customer.setMobileNumber(request.getMobileNumber());
    customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    customer.setPanNumber(request.getPanNumber());
    customer.setDateOfBirth(request.getDateOfBirth());
    customer.setAddress(request.getAddress());
    customer.setStatus(CustomerStatus.ACTIVE);

    Customer saved = customerRepository.save(customer);
    return mapToResponse(saved);
  }

  @Override
  public com.mypolicy.customer.dto.AuthResponse login(LoginRequest request) {
    Customer customer = customerRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("Invalid credentials"));

    if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
      throw new RuntimeException("Invalid credentials");
    }

    String token = jwtService.generateToken(customer.getEmail());
    return new com.mypolicy.customer.dto.AuthResponse(token, mapToResponse(customer));
  }

  @Override
  public CustomerResponse getCustomerByPanNumber(String panNumber) {
    return customerRepository.findByPanNumber(panNumber)
        .map(this::mapToResponse)
        .orElseThrow(() -> new RuntimeException("Customer not found with PAN: " + panNumber));
  }

  @Override
  public CustomerResponse getCustomerById(String customerId) {
    return customerRepository.findById(customerId)
        .map(this::mapToResponse)
        .orElseThrow(() -> new RuntimeException("Customer not found with id: " + customerId));
  }

  @Override
  @Transactional
<<<<<<< HEAD
  public CustomerResponse correctCustomer(String customerId, CustomerCorrectionRequest request) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found with id: " + customerId));

    if (request.getFirstName() != null && !request.getFirstName().isBlank())
      customer.setFirstName(request.getFirstName());
    if (request.getLastName() != null && !request.getLastName().isBlank())
      customer.setLastName(request.getLastName());
    if (request.getMobileNumber() != null && !request.getMobileNumber().isBlank()) {
      if (customerRepository.existsByMobileNumber(request.getMobileNumber())
          && !request.getMobileNumber().equals(customer.getMobileNumber()))
        throw new RuntimeException("Mobile number already in use");
      customer.setMobileNumber(request.getMobileNumber());
    }
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      if (customerRepository.existsByEmail(request.getEmail())
          && !request.getEmail().equals(customer.getEmail()))
        throw new RuntimeException("Email already in use");
      customer.setEmail(request.getEmail());
    }
    if (request.getAddress() != null) customer.setAddress(request.getAddress());
    if (request.getPanNumber() != null) customer.setPanNumber(request.getPanNumber());
    if (request.getDateOfBirth() != null) customer.setDateOfBirth(request.getDateOfBirth());

    Customer saved = customerRepository.save(customer);
    return mapToResponse(saved);
=======
  public CustomerResponse updateCustomer(String customerId, CustomerUpdateRequest request) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found with id: " + customerId));

    // Update only non-null fields
    if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
      customer.setFirstName(request.getFirstName());
    }

    if (request.getLastName() != null && !request.getLastName().isEmpty()) {
      customer.setLastName(request.getLastName());
    }

    if (request.getEmail() != null && !request.getEmail().isEmpty()) {
      // Check if new email is already taken by another customer
      customerRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
        if (!existing.getCustomerId().equals(customerId)) {
          throw new RuntimeException("Email already exists for another customer");
        }
      });
      customer.setEmail(request.getEmail());
    }

    if (request.getMobileNumber() != null && !request.getMobileNumber().isEmpty()) {
      // Check if new mobile is already taken by another customer
      customerRepository.findByMobileNumber(request.getMobileNumber()).ifPresent(existing -> {
        if (!existing.getCustomerId().equals(customerId)) {
          throw new RuntimeException("Mobile number already exists for another customer");
        }
      });
      customer.setMobileNumber(request.getMobileNumber());
    }

    if (request.getPanNumber() != null && !request.getPanNumber().isEmpty()) {
      // Check if new PAN is already taken by another customer
      customerRepository.findByPanNumber(request.getPanNumber()).ifPresent(existing -> {
        if (!existing.getCustomerId().equals(customerId)) {
          throw new RuntimeException("PAN number already exists for another customer");
        }
      });
      customer.setPanNumber(request.getPanNumber());
    }

    if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
      customer.setDateOfBirth(request.getDateOfBirth());
    }

    if (request.getAddress() != null && !request.getAddress().isEmpty()) {
      customer.setAddress(request.getAddress());
    }

    Customer updated = customerRepository.save(customer);
    return mapToResponse(updated);
>>>>>>> upstream/main
  }

  private CustomerResponse mapToResponse(Customer c) {
    return CustomerResponse.builder()
        .customerId(c.getCustomerId())
        .firstName(c.getFirstName())
        .lastName(c.getLastName())
        .email(c.getEmail())
        .mobileNumber(c.getMobileNumber())
        .status(c.getStatus())
        .panNumber(c.getPanNumber())
        .dateOfBirth(c.getDateOfBirth())
        .address(c.getAddress())
        .build();
  }
}
