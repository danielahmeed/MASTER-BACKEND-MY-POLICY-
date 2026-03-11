package com.mypolicy.customer.service.impl;

import com.mypolicy.customer.dto.CustomerBulkCreateRequest;
import com.mypolicy.customer.dto.CustomerBulkCreateResponse;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

  @Override
  public Optional<CustomerResponse> findByMobileNumber(String mobile) {
    return customerRepository.findByMobileNumber(mobile).map(this::mapToResponse);
  }

  @Override
  public Optional<CustomerResponse> findByEmail(String email) {
    return customerRepository.findByEmail(email).map(this::mapToResponse);
  }

  @Override
  public Optional<CustomerResponse> findByPanNumber(String pan) {
    return customerRepository.findByPanNumber(pan).map(this::mapToResponse);
  }

  @Override
  @Transactional
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
      try {
        customer.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
      } catch (Exception ignored) { /* keep existing DOB on parse error */ }
    }

    if (request.getAddress() != null && !request.getAddress().isEmpty()) {
      customer.setAddress(request.getAddress());
    }

    Customer updated = customerRepository.save(customer);
    return mapToResponse(updated);
  }

  private static final String BULK_IMPORT_DEFAULT_PASSWORD = "BulkImport@123";

  @Override
  @Transactional
  public CustomerBulkCreateResponse bulkCreateCustomers(List<CustomerBulkCreateRequest> requests) {
    int created = 0;
    int skipped = 0;
    List<String> createdIds = new ArrayList<>();
    List<CustomerBulkCreateResponse.CreatedCustomerInfo> createdCustomers = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    for (int i = 0; i < requests.size(); i++) {
      CustomerBulkCreateRequest req = requests.get(i);
      int rowNum = i + 1;
      try {
        if (customerRepository.existsByEmail(req.getEmail())) {
          skipped++;
          errors.add("Row " + rowNum + ": Email already exists");
          continue;
        }
        if (customerRepository.existsByMobileNumber(req.getMobileNumber())) {
          skipped++;
          errors.add("Row " + rowNum + ": Mobile number already exists");
          continue;
        }
        String password = (req.getPassword() != null && !req.getPassword().isBlank())
            ? req.getPassword()
            : BULK_IMPORT_DEFAULT_PASSWORD;

        Customer customer = new Customer();
        customer.setFirstName(req.getFirstName());
        customer.setLastName(req.getLastName());
        customer.setEmail(req.getEmail());
        customer.setMobileNumber(req.getMobileNumber());
        customer.setPasswordHash(passwordEncoder.encode(password));
        customer.setPanNumber(req.getPanNumber());
        customer.setDateOfBirth(req.getDateOfBirth());
        String addr = req.getAddress();
        if (req.getCity() != null && !req.getCity().isBlank()) {
          addr = (addr != null ? addr + ", " : "") + req.getCity();
        }
        customer.setAddress(addr);
        customer.setStatus(CustomerStatus.ACTIVE);

        Customer saved = customerRepository.save(customer);
        created++;
        createdIds.add(saved.getCustomerId());
        createdCustomers.add(CustomerBulkCreateResponse.CreatedCustomerInfo.builder()
            .customerId(saved.getCustomerId())
            .email(saved.getEmail())
            .build());
      } catch (Exception e) {
        skipped++;
        errors.add("Row " + rowNum + ": " + e.getMessage());
      }
    }

    return CustomerBulkCreateResponse.builder()
        .totalRequested(requests.size())
        .created(created)
        .skipped(skipped)
        .createdCustomerIds(createdIds)
        .createdCustomers(createdCustomers)
        .errors(errors)
        .build();
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
