package com.northbank.customerservice.mapper;

import com.northbank.customerservice.dto.response.AdminResponse;
import com.northbank.customerservice.entity.Customer;
import com.northbank.customerservice.entity.Role;

public class AdminMapper {
    private AdminMapper() {}   // no instances

    public static AdminResponse toResponse(Customer customer, Role role) {

        return new AdminResponse( CustomerMapper.toResponse(customer), role == null ? null : role.name());
    }
}
