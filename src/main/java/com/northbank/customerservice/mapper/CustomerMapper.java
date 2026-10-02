package com.northbank.customerservice.mapper;

import com.northbank.customerservice.dto.response.CustomerResponse;
import com.northbank.customerservice.entity.Customer;

public class CustomerMapper {
    public static CustomerResponse toResponse(Customer customer) {

        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getDateOfBirth(),
                customer.getStatus(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getCity(),
                customer.getGender(),
                customer.getPostcode()
        );
    }
}
