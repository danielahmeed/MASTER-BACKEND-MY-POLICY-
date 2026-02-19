package com.mypolicy.customer.service;

import com.mypolicy.customer.dto.AuthResponse;
import com.mypolicy.customer.dto.CustomerCorrectionRequest;
import com.mypolicy.customer.dto.CustomerRegistrationRequest;
import com.mypolicy.customer.dto.CustomerResponse;
import com.mypolicy.customer.dto.CustomerUpdateRequest;
import com.mypolicy.customer.dto.LoginRequest;

public interface CustomerService {
  CustomerResponse registerCustomer(CustomerRegistrationRequest request);

  AuthResponse login(LoginRequest request);

  CustomerResponse getCustomerById(String customerId);

<<<<<<< HEAD
  CustomerResponse getCustomerByPanNumber(String panNumber);

  CustomerResponse correctCustomer(String customerId, CustomerCorrectionRequest request);
=======
  CustomerResponse updateCustomer(String customerId, CustomerUpdateRequest request);
>>>>>>> upstream/main
}
