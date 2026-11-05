package com.northbank.customerservice.repository;

import com.northbank.customerservice.entity.Credentials;
import com.northbank.customerservice.entity.Role;
import com.northbank.customerservice.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CredentialsRepository extends JpaRepository<Credentials, UUID> {

    boolean existsByRole(Role role);
    boolean existsByRoleAndCustomer_Status(Role role, Status status);
}
