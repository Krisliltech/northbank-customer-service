package com.northbank.customerservice.service.customer;

import com.northbank.customerservice.dto.request.LoginRequest;
import com.northbank.customerservice.dto.request.RegisterRequest;
import com.northbank.customerservice.dto.request.UpdateRequest;
import com.northbank.customerservice.dto.response.AuthResponse;
import com.northbank.customerservice.dto.response.CustomerResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerResponse register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    CustomerResponse getCustomerById(UUID id);
    List<CustomerResponse> getCustomers();
    CustomerResponse updateCustomer(UpdateRequest updateRequest, UUID id);
}
