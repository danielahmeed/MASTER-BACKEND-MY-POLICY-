package com.mypolicy.customer.service;

import com.mypolicy.customer.dto.AuthResponse;
import com.mypolicy.customer.dto.CustomerBulkCreateRequest;
import com.mypolicy.customer.dto.CustomerBulkCreateResponse;
import com.mypolicy.customer.dto.CustomerRegistrationRequest;
import com.mypolicy.customer.dto.CustomerResponse;
import com.mypolicy.customer.dto.CustomerUpdateRequest;
import com.mypolicy.customer.dto.LoginRequest;

import java.util.List;
import java.util.Optional;

public interface CustomerService {
  CustomerResponse registerCustomer(CustomerRegistrationRequest request);

  AuthResponse login(LoginRequest request);

  CustomerResponse getCustomerById(String customerId);

  Optional<CustomerResponse> findByMobileNumber(String mobile);

  Optional<CustomerResponse> findByEmail(String email);

  Optional<CustomerResponse> findByPanNumber(String pan);

  CustomerResponse updateCustomer(String customerId, CustomerUpdateRequest request);

  /**
   * Bulk create customers (e.g. from CSV import). Uses default password if not provided.
   */
  CustomerBulkCreateResponse bulkCreateCustomers(List<CustomerBulkCreateRequest> requests);
}
