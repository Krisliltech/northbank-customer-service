package com.northbank.customerservice.controller;

import com.northbank.customerservice.dto.request.CustomerFilter;
import com.northbank.customerservice.dto.request.RoleRequest;
import com.northbank.customerservice.dto.response.AdminResponse;
import com.northbank.customerservice.dto.response.CustomerResponse;
import com.northbank.customerservice.dto.response.PageResponse;
import com.northbank.customerservice.exception.*;
import com.northbank.customerservice.security.JwtAuthFilter;
import com.northbank.customerservice.security.JwtAuthenticationEntryPoint;
import com.northbank.customerservice.security.JwtService;
import com.northbank.customerservice.security.SecurityConfig;
import com.northbank.customerservice.service.customer.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtAuthenticationEntryPoint.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hmac-sha-256-signing",
        "jwt.expiration=900000"
})
class CustomerControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @MockitoBean
    CustomerService customerService;

    private static final String VALID_REGISTER_BODY = """
            {"firstName":"John","lastName":"Doe","email":"john.doe@example.com",
             "password":"Str0ng!Pass","dateOfBirth":"1990-05-15"}""";

    private String bearer(UUID id, String role) {
        return "Bearer " + jwtService.generateToken(id, "user@example.com", role);
    }

    private CustomerResponse profile(UUID id) {
        CustomerResponse response = new CustomerResponse();
        response.setId(id);
        response.setFirstName("John");
        response.setEmail("john.doe@example.com");
        return response;
    }


    @Test
    void getCustomer_withoutToken_returns401WithJsonBody() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required. Please provide a valid token."))
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(customerService);
    }

    @Test
    void getCustomer_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid authentication token."));

        verifyNoInteractions(customerService);
    }

    @Test
    void getCustomer_asOwner_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.getCustomerById(id)).thenReturn(profile(id));

        mockMvc.perform(get("/api/customers/{id}", id)
                        .header("Authorization", bearer(id, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void getCustomer_asDifferentCustomer_returns403AndNeverReachesService() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();

        mockMvc.perform(get("/api/customers/{id}", owner)
                        .header("Authorization", bearer(stranger, "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));

        verifyNoInteractions(customerService);
    }

    @Test
    void getCustomer_asAdmin_canReadAnyProfile() throws Exception {
        UUID target = UUID.randomUUID();
        when(customerService.getCustomerById(target)).thenReturn(profile(target));

        mockMvc.perform(get("/api/customers/{id}", target)
                        .header("Authorization", bearer(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void listCustomers_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", bearer(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(customerService);
    }

    @Test
    void listCustomers_asAdmin_returns200WithPage() throws Exception {
        when(customerService.getCustomers(anyInt(), anyInt(), anyString(), anyString(), any(CustomerFilter.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", bearer(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void changeRole_asCustomer_returns403() throws Exception {
        mockMvc.perform(patch("/api/customers/{id}/role", UUID.randomUUID())
                        .header("Authorization", bearer(UUID.randomUUID(), "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(customerService);
    }

    @Test
    void changeRole_asAdmin_returns200WithRole() throws Exception {
        UUID target = UUID.randomUUID();
        when(customerService.changeRoleByAdmin(any(UUID.class), any(RoleRequest.class)))
                .thenReturn(new AdminResponse(profile(target), "ADMIN"));

        mockMvc.perform(patch("/api/customers/{id}/role", target)
                        .header("Authorization", bearer(UUID.randomUUID(), "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void register_withoutToken_returns201() throws Exception {
        when(customerService.register(any())).thenReturn(profile(UUID.randomUUID()));

        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void register_withWeakPasswordAndBadEmail_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","email":"not-an-email",
                                 "password":"password","dateOfBirth":"1990-05-15"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(customerService);
    }

    @Test
    void register_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    void getCustomer_withInvalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/customers/not-a-uuid")
                        .header("Authorization", bearer(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isBadRequest());
    }


    @Test
    void register_withExistingEmail_returns409() throws Exception {
        when(customerService.register(any())).thenThrow(new DuplicateEmailException("Email already in use"));

        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void register_withExistingPhoneNumber_returns409() throws Exception {
        when(customerService.register(any())).thenThrow(new DuplicatePhoneNumberException("Phone number already in use"));

        mockMvc.perform(post("/api/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Phone number already in use"));
    }

    @Test
    void login_withWrongCredentials_returns401() throws Exception {
        when(customerService.login(any())).thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"john.doe@example.com\",\"password\":\"Wrong1!pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void getCustomer_forUnknownId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.getCustomerById(id)).thenThrow(new CustomerNotFoundException("Customer not found"));

        mockMvc.perform(get("/api/customers/{id}", id)
                        .header("Authorization", bearer(id, "CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    @Test
    void getCustomer_onInactiveAccount_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.getCustomerById(id))
                .thenThrow(new AccountNotActiveException("Your account is suspended. Please contact support."));

        mockMvc.perform(get("/api/customers/{id}", id)
                        .header("Authorization", bearer(id, "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your account is suspended. Please contact support."));
    }
}
