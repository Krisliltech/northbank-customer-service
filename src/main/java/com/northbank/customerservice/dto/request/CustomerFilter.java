package com.northbank.customerservice.dto.request;

import com.northbank.customerservice.entity.Role;
import com.northbank.customerservice.entity.Status;

public record CustomerFilter(Status status, Role role, String search) {
}
