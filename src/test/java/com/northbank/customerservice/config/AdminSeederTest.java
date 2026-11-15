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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "Adm1n!Passw0rd";

    @Mock CustomerRepository customerRepository;
    @Mock CredentialsRepository credentialsRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock Validator validator;
    @Mock ConstraintViolation<RegisterRequest> violation;


    private AdminSeeder seederWith(String email, String password) {
        return new AdminSeeder(customerRepository, credentialsRepository,
                passwordEncoder, validator, email, password);
    }


    @Test
    void run_whenAnActiveAdminExists_ignoresEnvironmentValuesAndDoesNothing() {
        when(credentialsRepository.existsByRoleAndCustomer_Status(Role.ADMIN, Status.ACTIVE)).thenReturn(true);
        AdminSeeder seeder = seederWith(EMAIL, "weak");

        assertThatCode(seeder::run).doesNotThrowAnyException();
//        assertThatCode(() -> seeder.run()).doesNotThrowAnyException();
        verify(customerRepository, never()).save(any());
        verify(credentialsRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, validator);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "'', ''",
            "admin@example.com, ''",
            "'', Adm1n!Passw0rd",
            "'   ', Adm1n!Passw0rd"
    })
    void run_withoutAdminCredentialsInEnvironment_createsNothing(String email, String password) {
        AdminSeeder seeder = seederWith(email, password);

        assertThatCode(seeder::run).doesNotThrowAnyException();

        verify(customerRepository, never()).save(any());
        verify(credentialsRepository, never()).save(any());
        verifyNoInteractions(validator, passwordEncoder, customerRepository);
    }

    @Test
    void run_withPasswordBreakingThePolicy_failsStartupWithoutLeakingThePassword() {
        when(violation.getMessage()).thenReturn("Password must contain an uppercase letter");
        when(validator.validateValue(RegisterRequest.class, "password", "x-secret-marker"))
                .thenReturn(Set.of(violation));
        AdminSeeder seeder = seederWith(EMAIL, "x-secret-marker");

        assertThatThrownBy(seeder::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD rejected")
                .hasMessageContaining("Password must contain an uppercase letter")
                .hasMessageNotContaining("x-secret-marker");

        verify(customerRepository, never()).save(any());
        verify(credentialsRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_whenEmailBelongsToExistingAccount_createsNothingAndDoesNotFailStartup() {
        when(validator.validateValue(RegisterRequest.class, "password", PASSWORD)).thenReturn(Set.of());
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new Customer()));
        AdminSeeder seeder = seederWith(EMAIL, PASSWORD);

        assertThatCode(seeder::run).doesNotThrowAnyException();

        verify(customerRepository, never()).save(any());
        verify(credentialsRepository, never()).save(any());
        verify(credentialsRepository, never()).findById(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_whenNoAdminExists_savesCustomerThenAdminCredentialsWithHashedPassword() {
        when(validator.validateValue(RegisterRequest.class, "password", PASSWORD)).thenReturn(Set.of());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("admin-hash");
        AdminSeeder seeder = seederWith(EMAIL, PASSWORD);

        seeder.run();

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        ArgumentCaptor<Credentials> credentialsCaptor = ArgumentCaptor.forClass(Credentials.class);
        InOrder inOrder = inOrder(customerRepository, credentialsRepository);
        inOrder.verify(customerRepository).save(customerCaptor.capture());
        inOrder.verify(credentialsRepository).save(credentialsCaptor.capture());

        Customer customer = customerCaptor.getValue();
        Credentials credentials = credentialsCaptor.getValue();

        assertThat(customer.getEmail()).isEqualTo(EMAIL);
        assertThat(customer.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(credentials.getRole()).isEqualTo(Role.ADMIN);
        assertThat(credentials.getPassword()).isEqualTo("admin-hash");
        assertThat(credentials.getId()).isEqualTo(customer.getId());
        verify(validator).validateValue(RegisterRequest.class, "password", PASSWORD);
    }

    @Test
    void run_whenAdminsExistButNoneIsActive_createsAFreshAdmin() {
        when(credentialsRepository.existsByRole(Role.ADMIN)).thenReturn(true);
        when(validator.validateValue(RegisterRequest.class, "password", PASSWORD)).thenReturn(Set.of());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("admin-hash");
        AdminSeeder seeder = seederWith(EMAIL, PASSWORD);

        seeder.run();

        ArgumentCaptor<Credentials> captor = ArgumentCaptor.forClass(Credentials.class);
        verify(credentialsRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void run_normalisesTheConfiguredEmail() {
        when(validator.validateValue(RegisterRequest.class, "password", PASSWORD)).thenReturn(Set.of());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("admin-hash");
        AdminSeeder seeder = seederWith("  Admin@Example.COM  ", PASSWORD);

        seeder.run();

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).findByEmail("admin@example.com");
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@example.com");
    }
}
