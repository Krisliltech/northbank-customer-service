package com.northbank.customerservice.service.customer;

import com.northbank.customerservice.dto.request.*;
import com.northbank.customerservice.dto.response.AdminResponse;
import com.northbank.customerservice.dto.response.AuthResponse;
import com.northbank.customerservice.dto.response.CustomerResponse;
import com.northbank.customerservice.dto.response.PageResponse;
import com.northbank.customerservice.entity.Credentials;
import com.northbank.customerservice.entity.Customer;
import com.northbank.customerservice.entity.Role;
import com.northbank.customerservice.entity.Status;
import com.northbank.customerservice.exception.*;
import com.northbank.customerservice.mapper.AdminMapper;
import com.northbank.customerservice.mapper.CustomerMapper;
import com.northbank.customerservice.repository.CredentialsRepository;
import com.northbank.customerservice.repository.CustomerRepository;
import com.northbank.customerservice.repository.CustomerSpecifications;
import com.northbank.customerservice.security.JwtService;
import com.northbank.customerservice.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);
    private final String dummyHash;
    private static final int MAX_SEARCH_LENGTH = 100;
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "createdAt", "createdAt",
            "firstName", "firstName",
            "lastName", "lastName",
            "email", "email"
    );

    public CustomerServiceImpl(CustomerRepository customerRepository, CredentialsRepository credentialsRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
    }

    @Override
    public CustomerResponse register(RegisterRequest request) {
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already in use");
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (customerRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
                throw new DuplicatePhoneNumberException("Phone number already in use");
            }
        }

        UUID customerId = UUID.randomUUID();

        Customer customer = new Customer(
                customerId,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getDateOfBirth(),
                request.getAddress(),
                request.getCity(),
                request.getPostcode(),
                request.getPhoneNumber(),
                request.getGender()
        );

        customerRepository.save(customer);
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        Credentials credentials = new Credentials(
                customerId,
                customer,
                hashedPassword
        );

        credentialsRepository.save(credentials);
        return CustomerMapper.toResponse(customer);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        AuthResult authResult = authenticateCustomer(loginRequest.getEmail(), loginRequest.getPassword());

        Customer customer = authResult.customer();
        Credentials credentials = authResult.credentials();

        assertActive(customer);
        UUID customerId = customer.getId();
        String roleName = credentials.getRole().name();
        String token = jwtService.generateToken(customerId, customer.getEmail(), roleName);

        return new AuthResponse(
                token,
                customerId,
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                roleName
        );
    }

    @Override
    public CustomerResponse getCustomerById(UUID id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            throw new CustomerNotFoundException("Customer not found");
        }
        Customer existingCustomer = customer.get();
        if (!SecurityUtils.isCallerAdmin()) {
            assertActive(existingCustomer);
        }
        return CustomerMapper.toResponse(existingCustomer);
    }

    @Override
    public PageResponse<AdminResponse> getCustomers(int page, int size, String sortBy, String direction, CustomerFilter filter) {
        String field = SORTABLE_FIELDS.get(sortBy);
        if (field == null) {
            throw new InvalidRequestException(
                    "Invalid sortBy. Allowed values: createdAt, firstName, lastName, email");
        }

        Sort.Direction dir;
        if ("asc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.DESC;
        } else {
            throw new InvalidRequestException("Invalid direction. Allowed values: asc, desc");
        }

        if (filter.search() != null && filter.search().strip().length() > MAX_SEARCH_LENGTH) {
            throw new InvalidRequestException("Search term must be at most 100 characters");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, field));
        Page<Customer> customers = customerRepository
                .findAll(CustomerSpecifications.fromFilter(filter), pageable);

        List<UUID> ids = customers.getContent().stream()
                .map(Customer::getId)
                .toList();

        Map<UUID, Role> rolesById = credentialsRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Credentials::getId, Credentials::getRole));

        Page<AdminResponse> mapped = customers
                .map(c -> AdminMapper.toResponse(c, rolesById.get(c.getId())));

        return PageResponse.from(mapped);
//        return customerRepository.findAll()
//                .stream()
//                .map(CustomerMapper::toResponse)
//      .map(customer -> CustomerMapper.toResponse(customer)) this is the same thing as the previous line
//                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponse updateCustomer(UpdateRequest updateRequest, UUID id) {
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        assertActive(existingCustomer);

        if (updateRequest.getFirstName() != null) {
            existingCustomer.setFirstName(updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null) {
            existingCustomer.setLastName(updateRequest.getLastName());
        }

        if (updateRequest.getDateOfBirth() != null) {
            existingCustomer.setDateOfBirth(updateRequest.getDateOfBirth());
        }

        if (updateRequest.getAddress() != null) {
            existingCustomer.setAddress(updateRequest.getAddress());
        }

        if (updateRequest.getCity() != null) {
            existingCustomer.setCity(updateRequest.getCity());
        }

        if (updateRequest.getPostcode() != null) {
            existingCustomer.setPostcode(updateRequest.getPostcode());
        }

        if (updateRequest.getPhoneNumber() != null) {
            Optional<Customer> customerWithPhone = customerRepository.findByPhoneNumber(updateRequest.getPhoneNumber());
            if (customerWithPhone.isPresent() && !customerWithPhone.get().getId().equals(id)) {
                throw new DuplicatePhoneNumberException("Phone number already in use");
            }
            existingCustomer.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        if (updateRequest.getGender() != null) {
            existingCustomer.setGender(updateRequest.getGender());
        }

        customerRepository.save(existingCustomer);

        return CustomerMapper.toResponse(existingCustomer);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest changePasswordRequest, UUID id) {
        Credentials credentials = credentialsRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        Customer existingCustomer = credentials.getCustomer();
        assertActive(existingCustomer);

        boolean oldPasswordCheck = passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), credentials.getPassword());
        if (!oldPasswordCheck) {
            throw new InvalidRequestException("Current password is incorrect");
        }

        if (changePasswordRequest.getCurrentPassword().equals(changePasswordRequest.getNewPassword())) {
            throw new InvalidRequestException("New password must be different from the current password");
        }

        String hashedPassword = passwordEncoder.encode(changePasswordRequest.getNewPassword());
        credentials.setPassword(hashedPassword);
//        credentialsRepository.save(credentials);
        log.info("Password changed for customer {}", id);
    }

    private void assertActive(Customer customer) {
        if (customer.getStatus() != Status.ACTIVE) {
            log.warn("Blocked access to non-active account {} (status={})",
                    customer.getId(), customer.getStatus());
            throw new AccountNotActiveException("Your account is not active. Please contact support.");
        }
    }

    private CustomerResponse changeStatus(Customer customer, Status status) {
        Status oldStatus = customer.getStatus();
        if (status == oldStatus) {
            throw new InvalidRequestException("Customer is already " + status);
        }

        if (!oldStatus.canTransitionTo(status)){
            throw new InvalidStatusTransitionException("Cannot change status from " + oldStatus + " to " + status);
        }

        customer.setStatus(status);
//        customerRepository.save(customer);
        log.info("Status changed: customer = {} {} -> {} by actor = {}",
                customer.getId(), oldStatus, status, SecurityUtils.currentActorId());

        return CustomerMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public void deActivate(UUID id, DeactivateRequest deactivateRequest) {
        Credentials credentials = credentialsRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        Customer existingCustomer = credentials.getCustomer();
        assertActive(existingCustomer);

        boolean passwordCheck = passwordEncoder.matches(deactivateRequest.getCurrentPassword(), credentials.getPassword());
        if (!passwordCheck) {
            throw new InvalidRequestException("Password is incorrect");
        }

       changeStatus(existingCustomer, Status.DEACTIVATED);
    }

    @Override
    @Transactional
    public CustomerResponse reActivate(LoginRequest reactivateRequest) {
        AuthResult authResult = authenticateCustomer(reactivateRequest.getEmail(), reactivateRequest.getPassword());

        Customer customer = authResult.customer();

        Status currentStatus = customer.getStatus();
        if (currentStatus == Status.ACTIVE) {
            throw new InvalidRequestException("Account is already active");
        }
        if (currentStatus != Status.DEACTIVATED) {
            throw new AccountNotActiveException(
                    "Your account cannot be reactivated online. Please contact support.");
        }

        return changeStatus(customer, Status.ACTIVE);
    }

    @Override
    @Transactional
    public CustomerResponse changeStatusByAdmin(UUID id, StatusRequest statusRequest) {
        if (id.equals(SecurityUtils.currentActorId())) {
            throw new InvalidRequestException("You cannot change your own account status");
        }

        Status status = statusRequest.getStatus();
        if (status == Status.DEACTIVATED) {
            throw new InvalidRequestException("Only the customer can deactivate their account");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        return changeStatus(customer, status);
    }

    private record AuthResult(Customer customer, Credentials credentials) {}

    private AuthResult authenticateCustomer(String email, String rawPassword) {
        Optional<Customer> customer = customerRepository.findByEmail(email);
        Optional<Credentials> credentials = customer.flatMap(c -> credentialsRepository.findById(c.getId()));

        String hashToCheck = credentials.map(Credentials::getPassword).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(rawPassword, hashToCheck);

        if (customer.isEmpty() || credentials.isEmpty() || !matches) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return new AuthResult(customer.get(), credentials.get());
    }

    @Override
    @Transactional
    public AdminResponse changeRoleByAdmin(UUID id, RoleRequest roleRequest) {
        if (id.equals(SecurityUtils.currentActorId())) {
            throw new InvalidRequestException("You cannot change your own account role");
        }

        Credentials credentials = credentialsRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        Customer customer = credentials.getCustomer();
        assertActive(customer);

        Role oldRole = credentials.getRole();
        Role newRole = roleRequest.getRole();
        if (newRole == oldRole) {
            throw new InvalidRequestException("Account role is already " + roleRequest.getRole());
        }

        credentials.setRole(roleRequest.getRole());
        log.warn("Role changed: customer={} {} -> {} by actor={}",
                id, oldRole, newRole, SecurityUtils.currentActorId());
        return AdminMapper.toResponse(customer, credentials.getRole());
    }
}
