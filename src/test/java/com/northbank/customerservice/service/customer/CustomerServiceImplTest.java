package com.northbank.customerservice.service.customer;

import com.northbank.customerservice.dto.request.ChangePasswordRequest;
import com.northbank.customerservice.dto.request.LoginRequest;
import com.northbank.customerservice.dto.request.RegisterRequest;
import com.northbank.customerservice.dto.response.AuthResponse;
import com.northbank.customerservice.dto.response.CustomerResponse;
import com.northbank.customerservice.entity.Credentials;
import com.northbank.customerservice.entity.Customer;
import com.northbank.customerservice.entity.Role;
import com.northbank.customerservice.entity.Status;
import com.northbank.customerservice.exception.*;
import com.northbank.customerservice.repository.CredentialsRepository;
import com.northbank.customerservice.repository.CustomerRepository;
import com.northbank.customerservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {
    @Mock
    CustomerRepository customerRepository;
    @Mock
    CredentialsRepository credentialsRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;

    @InjectMocks
    CustomerServiceImpl customerServiceImpl;

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setPassword("Str0ng!Pass");
        request.setDateOfBirth(LocalDate.of(1990, 5, 15));
        return request;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john.doe@example.com");
        request.setPassword("Str0ng!Pass");
        return request;
    }

    private ChangePasswordRequest changePasswordRequest(String current, String password) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(password);
        return request;
    }

    private Customer customerWithStatus(UUID id, Status status) {
        Customer customer = new Customer(id, "John", "Doe", "john.doe@example.com",
                LocalDate.of(1990, 5, 15), null, null, null, null, null);
        customer.setStatus(status);
        return customer;
    }

    @Test
    void register_withNewEmail_savesHashedCredentialsWithCustomerRole() {
        RegisterRequest request = validRequest();
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");

        CustomerResponse response = customerServiceImpl.register(request);

        ArgumentCaptor<Credentials> captor = ArgumentCaptor.forClass(Credentials.class);
        verify(credentialsRepository).save(captor.capture());
        Credentials saved = captor.getValue();

        assertThat(saved.getPassword()).isEqualTo("hashed-password");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getId()).isNotNull();
        assertThat(response.getId()).isEqualTo(saved.getId());
    }

    @Test
    void register_withAnAlreadyExistingEmail_throwsDuplicateEmailException() {
        RegisterRequest request = validRequest();

        when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new Customer()));

        assertThatThrownBy(() -> customerServiceImpl.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Email already in use");

        verify(customerRepository).findByEmail("john.doe@example.com");
        verify(customerRepository, never()).save(any());
        verifyNoInteractions(credentialsRepository);
    }

    @Test
    void register_withAnAlreadyExistingPhoneNumber_throwsDuplicatePhoneNumberException() {
        RegisterRequest request = validRequest();
        request.setPhoneNumber("07123456789");
        when(customerRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.of(new Customer()));

        assertThatThrownBy(() -> customerServiceImpl.register(request))
                .isInstanceOf(DuplicatePhoneNumberException.class)
                .hasMessage("Phone number already in use");

        verify(customerRepository).findByPhoneNumber(request.getPhoneNumber());
        verify(customerRepository, never()).save(any());
        verifyNoInteractions(credentialsRepository);
    }

    @Test
    void login_withCorrectCredentials_returnsTokenAndCustomerDetails() {
        LoginRequest request = loginRequest();
        UUID id = UUID.randomUUID();

        Customer customer = customerWithStatus(id, Status.ACTIVE);
        Credentials credentials = new Credentials(id, customer, "stored-hash");
        credentials.setRole(Role.CUSTOMER);

        when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(customer));
        when(credentialsRepository.findById(id)).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(eq(request.getPassword()), any())).thenReturn(true);
        when(jwtService.generateToken(id, request.getEmail(), credentials.getRole().name())).thenReturn("jwt-token");

        AuthResponse response = customerServiceImpl.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        verify(credentialsRepository, never()).save(any());
    }

    @Test
    void login_withWrongEmail_throwsInvalidCredentialsException() {
        LoginRequest request = loginRequest();

        when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(eq(request.getPassword()), any())).thenReturn(false);

        assertThatThrownBy(() -> customerServiceImpl.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder).matches(eq(request.getPassword()), any());
        verify(customerRepository).findByEmail(request.getEmail());
        verifyNoInteractions(jwtService);
        verifyNoInteractions(credentialsRepository);
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentialsException() {
        LoginRequest request = loginRequest();
        request.setPassword("random-password");

        UUID id = UUID.randomUUID();

        Customer customer = customerWithStatus(id, Status.ACTIVE);
        Credentials credentials = new Credentials(id, customer, "stored-hash");
        credentials.setRole(Role.CUSTOMER);

        when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(customer));
        when(credentialsRepository.findById(id)).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(eq(request.getPassword()), any())).thenReturn(false);

        assertThatThrownBy(() -> customerServiceImpl.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder).matches(request.getPassword(), credentials.getPassword());
        verify(customerRepository).findByEmail(request.getEmail());
        verify(customerRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @ParameterizedTest
    @CsvSource({
            "DEACTIVATED, Your account is deactivated. You can reactivate it using the reactivate option.",
            "SUSPENDED, Your account is suspended. Please contact support.",
            "CLOSED, Your account is closed. Please contact support."
    })
    void login_onInactiveAcct_returnsStatusSpecificMessage(Status status, String expectedMessage) {
        LoginRequest request = loginRequest();
        UUID id = UUID.randomUUID();

        Customer customer = customerWithStatus(id, status);
        Credentials credentials = new Credentials(id, customer, "stored-hash");
        credentials.setRole(Role.CUSTOMER);

        when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(customer));
        when(credentialsRepository.findById(id)).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(eq(request.getPassword()), any())).thenReturn(true);

        assertThatThrownBy(() -> customerServiceImpl.login(request))
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessage(expectedMessage);

        verify(passwordEncoder).matches(request.getPassword(), credentials.getPassword());
        verify(customerRepository).findByEmail(request.getEmail());
        verify(customerRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_withWrongPasswordOnInactiveAccount_throwsInvalidCredentialsException() {
        LoginRequest request = loginRequest();
        UUID id = UUID.randomUUID();

        Customer customer = customerWithStatus(id, Status.SUSPENDED);
        Credentials credentials = new Credentials(id, customer, "stored-hash");
        credentials.setRole(Role.CUSTOMER);

        when(customerRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(customer));
        when(credentialsRepository.findById(id)).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(eq(request.getPassword()), any())).thenReturn(false);

        assertThatThrownBy(() -> customerServiceImpl.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder).matches(request.getPassword(), credentials.getPassword());
        verify(customerRepository).findByEmail(request.getEmail());
        verify(customerRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void changePassword_withCorrectPasswords_replacesStoredHash() {
        ChangePasswordRequest request = changePasswordRequest("Str0ng!Pass", "Str0ng!Pass1");
        UUID id = UUID.randomUUID();
        Credentials credentials = new Credentials(id, customerWithStatus(id, Status.ACTIVE), "old-hash");

        when(credentialsRepository.findById(id)).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(request.getCurrentPassword(), credentials.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("new-hash");


        customerServiceImpl.changePassword(request, id);

        assertThat(credentials.getPassword()).isEqualTo("new-hash");
        verify(passwordEncoder).matches(request.getCurrentPassword(), "old-hash");
        verify(passwordEncoder).encode(request.getNewPassword());
        verify(credentialsRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "DEACTIVATED, Your account is deactivated. You can reactivate it using the reactivate option.",
            "SUSPENDED, Your account is suspended. Please contact support.",
            "CLOSED, Your account is closed. Please contact support."
    })
    void changePassword_onInactiveAcct_returnsStatusSpecificMessage(Status status, String expectedMessage) {
        ChangePasswordRequest request = changePasswordRequest("Str0ng!Pass", "Str0ng!Pass1");
        UUID id = UUID.randomUUID();
        Credentials credentials = new Credentials(id, customerWithStatus(id, status), "old-hash");

        when(credentialsRepository.findById(id)).thenReturn(Optional.of(credentials));

        assertThatThrownBy(() -> customerServiceImpl.changePassword(request, id))
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessage(expectedMessage);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordEncoder, never()).encode("Str0ng!Pass1");
        verify(credentialsRepository, never()).save(any());
    }

    @Test
    void changePassword_withWrongCurrentPassword_throwsInvalidRequestException() {
        ChangePasswordRequest request = changePasswordRequest("Str0ng!Pass", "Str0ng!Pass1");
        UUID id = UUID.randomUUID();
        Credentials credentials = new Credentials(id, customerWithStatus(id, Status.ACTIVE), "old-hash");

        when(credentialsRepository.findById(id)).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(request.getCurrentPassword(), credentials.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> customerServiceImpl.changePassword(request, id))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Current password is incorrect");

        verify(passwordEncoder).matches("Str0ng!Pass", "old-hash");
        verify(passwordEncoder, never()).encode("Str0ng!Pass1");
        verify(credentialsRepository, never()).save(any());
    }

    @Test
    void changePassword_withCurrentPasswordEqualNewPassword_throwsInvalidRequestException() {
        ChangePasswordRequest request = changePasswordRequest("Str0ng!Pass!", "Str0ng!Pass!");
        UUID id = UUID.randomUUID();
        Credentials credentials = new Credentials(id, customerWithStatus(id, Status.ACTIVE), "old-hash");

        when(credentialsRepository.findById(id)).thenReturn(Optional.of(credentials));

        assertThatThrownBy(() -> customerServiceImpl.changePassword(request, id))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("New password must be different from the current password");

        verify(passwordEncoder, never()).matches("Str0ng!Pass", "Str0ng!Pass");
        verify(passwordEncoder, never()).encode("Str0ng!Pass");
        verify(credentialsRepository, never()).save(any());
    }

    @Test
    void changePassword_withUnknownId_throwsCustomerNotFoundException() {
        ChangePasswordRequest request = changePasswordRequest("Str0ng!Pass", "Str0ng!Pass1");
        UUID id = UUID.randomUUID();

        when(credentialsRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerServiceImpl.changePassword(request, id))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordEncoder, never()).encode("Str0ng!Pass1");
        verify(credentialsRepository, never()).save(any());
    }
}
