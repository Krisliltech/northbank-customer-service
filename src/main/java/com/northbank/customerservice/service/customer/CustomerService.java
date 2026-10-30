package com.northbank.customerservice.service.customer;

import com.northbank.customerservice.dto.request.*;
import com.northbank.customerservice.dto.response.AuthResponse;
import com.northbank.customerservice.dto.response.CustomerResponse;
import com.northbank.customerservice.dto.response.PageResponse;
import com.northbank.customerservice.entity.Status;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerResponse register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    CustomerResponse getCustomerById(UUID id);
    PageResponse<CustomerResponse> getCustomers(int page, int size);
    CustomerResponse updateCustomer(UpdateRequest updateRequest, UUID id);
    void changePassword(ChangePasswordRequest changePasswordRequest,  UUID id);
    void deActivate(UUID id, DeactivateRequest deactivateRequest);
    CustomerResponse reActivate(LoginRequest reactivateRequest);
    CustomerResponse changeStatusAsAdmin(UUID id, StatusRequest statusRequest);
}
