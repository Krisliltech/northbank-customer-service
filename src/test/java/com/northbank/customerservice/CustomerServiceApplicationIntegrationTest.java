package com.northbank.customerservice;

import com.jayway.jsonpath.JsonPath;
import com.northbank.customerservice.repository.CredentialsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret=integration-test-secret-key-that-is-long-enough-for-hmac-sha-256",
        "jwt.expiration=900000",
        "ADMIN_EMAIL=admin@example.com",
        "ADMIN_PASSWORD=Adm1n!Passw0rd"
})
class CustomerServiceApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String AUTH = "Authorization";
    private static final String PASSWORD = "Str0ng!Pass";
    private static final String NEW_PASSWORD = "N3w!Passw0rd";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "Adm1n!Passw0rd";

    @Autowired MockMvc mockMvc;
    @Autowired
    CredentialsRepository credentialsRepository;


    private String uniqueEmail() {
        return "it-" + UUID.randomUUID() + "@example.com";
    }

    private String registerBody(String email, String password) {
        return """
                {"firstName":"John","lastName":"Doe","email":"%s","password":"%s","dateOfBirth":"1990-05-15"}"""
                .formatted(email, password);
    }

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/customers/register")
                        .contentType(JSON).content(registerBody(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions attemptLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/customers/login").contentType(JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }

    private String loginToken(String email, String password) throws Exception {
        String body = attemptLogin(email, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }


    @Test
    void registerLoginAndReadOwnProfile() throws Exception {
        String email = uniqueEmail();
        String id = register(email);
        String token = loginToken(email, PASSWORD);

        mockMvc.perform(get("/api/customers/{id}", id).header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.hashedPassword").doesNotExist());

        String storedHash = credentialsRepository.findById(UUID.fromString(id)).orElseThrow().getPassword();
        assertThat(storedHash).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void register_withExistingEmail_returns409() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/customers/register").contentType(JSON)
                        .content(registerBody(email, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void login_withWrongPasswordOrUnknownEmail_returnsTheSameGeneric401() throws Exception {
        String email = uniqueEmail();
        register(email);

        attemptLogin(email, "Wr0ng!Password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        attemptLogin(uniqueEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void readingAnotherCustomersProfile_returns403() throws Exception {
        String emailA = uniqueEmail();
        register(emailA);
        String idB = register(uniqueEmail());
        String tokenA = loginToken(emailA, PASSWORD);

        mockMvc.perform(get("/api/customers/{id}", idB).header(AUTH, bearer(tokenA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void partialUpdate_isPersistedAndLeavesOtherFieldsAlone() throws Exception {
        String email = uniqueEmail();
        String id = register(email);
        String token = loginToken(email, PASSWORD);

        mockMvc.perform(patch("/api/customers/{id}", id).header(AUTH, bearer(token))
                        .contentType(JSON).content("{\"city\":\"Manchester\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Manchester"));

        mockMvc.perform(get("/api/customers/{id}", id).header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.city").value("Manchester"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void changePassword_isPersisted_oldPasswordStopsWorking() throws Exception {
        String email = uniqueEmail();
        String id = register(email);
        String token = loginToken(email, PASSWORD);

        mockMvc.perform(patch("/api/customers/{id}/password", id).header(AUTH, bearer(token))
                        .contentType(JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}""".formatted(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        attemptLogin(email, PASSWORD).andExpect(status().isUnauthorized());
        loginToken(email, NEW_PASSWORD);
    }

    @Test
    void deactivateAndReactivate_roundTrip() throws Exception {
        String email = uniqueEmail();
        String id = register(email);
        String token = loginToken(email, PASSWORD);

        mockMvc.perform(patch("/api/customers/{id}/deactivate", id).header(AUTH, bearer(token))
                        .contentType(JSON).content("{\"currentPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        attemptLogin(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Your account is deactivated. You can reactivate it using the reactivate option."));
        mockMvc.perform(get("/api/customers/{id}", id).header(AUTH, bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/customers/reactivate").contentType(JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        loginToken(email, PASSWORD);
    }

    @Test
    void suspendedCustomer_cannotReactivateThemselves() throws Exception {
        String email = uniqueEmail();
        String id = register(email);
        String adminToken = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(patch("/api/customers/{id}/status", id).header(AUTH, bearer(adminToken))
                        .contentType(JSON).content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(post("/api/customers/reactivate").contentType(JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your account is suspended. Please contact support."));
        attemptLogin(email, PASSWORD).andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/customers/{id}/status", id).header(AUTH, bearer(adminToken))
                        .contentType(JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        loginToken(email, PASSWORD);
    }

    @Test
    void admin_canListFilterAndSortCustomers() throws Exception {
        String marker = UUID.randomUUID().toString();
        String email = "it-" + marker + "@example.com";
        register(email);
        String adminToken = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/customers").param("search", marker).header(AUTH, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(email))
                .andExpect(jsonPath("$.content[0].role").value("CUSTOMER"));

        mockMvc.perform(get("/api/customers").param("role", "ADMIN").header(AUTH, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.email=='" + ADMIN_EMAIL + "')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.role=='CUSTOMER')]").isEmpty());

        mockMvc.perform(get("/api/customers").param("sortBy", "lastName").param("direction", "desc")
                        .header(AUTH, bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/customers").param("sortBy", "password").header(AUTH, bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_roleChange_takesEffectOnlyAfterReLogin() throws Exception {
        String email = uniqueEmail();
        String id = register(email);
        String oldToken = loginToken(email, PASSWORD);
        String adminToken = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(patch("/api/customers/{id}/role", id).header(AUTH, bearer(adminToken))
                        .contentType(JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("John"));

        mockMvc.perform(get("/api/customers").header(AUTH, bearer(oldToken)))
                .andExpect(status().isForbidden());

        String newToken = loginToken(email, PASSWORD);
        mockMvc.perform(get("/api/customers").header(AUTH, bearer(newToken)))
                .andExpect(status().isOk());
    }
}
