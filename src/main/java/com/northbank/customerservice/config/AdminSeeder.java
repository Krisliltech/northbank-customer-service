package com.northbank.customerservice.config;

import com.northbank.customerservice.dto.request.RegisterRequest;
import com.northbank.customerservice.entity.Credentials;
import com.northbank.customerservice.entity.Customer;
import com.northbank.customerservice.entity.Role;
import com.northbank.customerservice.entity.Status;
import com.northbank.customerservice.repository.CredentialsRepository;
import com.northbank.customerservice.repository.CustomerRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@NullMarked
@Component
public class AdminSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final CustomerRepository customerRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(CustomerRepository customerRepository,
                       CredentialsRepository credentialsRepository,
                       PasswordEncoder passwordEncoder,
                       Validator validator,
                       @Value("${ADMIN_EMAIL:}") String adminEmail,
                       @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.customerRepository = customerRepository;
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (credentialsRepository.existsByRoleAndCustomer_Status(Role.ADMIN, Status.ACTIVE)) {
            log.info("Admin seeding skipped: an active admin exists");
            return;
        }

        String situation = credentialsRepository.existsByRole(Role.ADMIN)
                ? "Admin accounts exist but none is active"
                : "No admin account exists";

        if (adminEmail.isBlank() || adminPassword.isEmpty()) {
            log.error("{} and ADMIN_EMAIL / ADMIN_PASSWORD are not set", situation);
            return;
        }

        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validateValue(RegisterRequest.class, "password", adminPassword);
        if (!violations.isEmpty()) {
            String reasons = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new IllegalStateException("ADMIN_PASSWORD rejected: " + reasons);
        }

        String email = adminEmail.strip().toLowerCase(Locale.ROOT);
        if (customerRepository.findByEmail(email).isPresent()) {
            log.error("{}. ADMIN_EMAIL already belongs to an existing account, so nothing was created. "
                    + "Use an unused email, or reactivate the admin directly in the database", situation);
            return;
        }

        UUID id = UUID.randomUUID();
        Customer customer = new Customer(id, "System", "Admin", email,
                LocalDate.of(1990, 1, 1), null, null, null, null, null);
        customerRepository.save(customer);

        Credentials credentials = new Credentials(id, customer, passwordEncoder.encode(adminPassword));
        credentials.setRole(Role.ADMIN);
        credentialsRepository.save(credentials);

        log.warn("{}. Created admin account: id={}", situation, id);
    }
}
