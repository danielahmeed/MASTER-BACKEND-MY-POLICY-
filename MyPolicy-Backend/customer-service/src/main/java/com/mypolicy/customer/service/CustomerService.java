package com.mypolicy.customer.service;

import com.mypolicy.customer.dto.AuthResponse;
import com.mypolicy.customer.dto.CustomerRegistrationRequest;
import com.mypolicy.customer.dto.CustomerResponse;
import com.mypolicy.customer.dto.CustomerUpdateRequest;
import com.mypolicy.customer.dto.LoginRequest;

public interface CustomerService {
  CustomerResponse registerCustomer(CustomerRegistrationRequest request);

  AuthResponse login(LoginRequest request);

  CustomerResponse getCustomerById(String customerId);

  CustomerResponse updateCustomer(String customerId, CustomerUpdateRequest request);
}
