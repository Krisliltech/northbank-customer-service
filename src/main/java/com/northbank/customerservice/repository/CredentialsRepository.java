package com.northbank.customerservice.repository;

import com.northbank.customerservice.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CredentialsRepository extends JpaRepository<Credentials, UUID> {}
