package com.mypolicy.customer.service.impl;

import com.mypolicy.customer.dto.CustomerRegistrationRequest;
import com.mypolicy.customer.dto.CustomerResponse;
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
  public CustomerResponse getCustomerById(String customerId) {
    return customerRepository.findById(customerId)
        .map(this::mapToResponse)
        .orElseThrow(() -> new RuntimeException("Customer not found with id: " + customerId));
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
