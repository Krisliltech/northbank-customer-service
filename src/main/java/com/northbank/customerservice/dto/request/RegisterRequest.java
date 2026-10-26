package com.northbank.customerservice.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class RegisterRequest {
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @NotBlank(message = "First name is required")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    @Pattern(regexp = "^\\S(.*\\S)?$", message = "Password must not start or end with whitespace")
    private String password;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(min = 1, max = 255, message = "Address must be between 1 and 255 characters")
    private String address;

    @Size(min = 1, max = 100, message = "City must be between 1 and 100 characters")
    private String city;

    @Size(min = 5, max = 10, message = "Postcode must be between 5 and 10 characters")
    private String postcode;

    @Size(min = 5, max = 20, message = "Phone number must be between 5 and 20 characters")
    @Pattern(
            regexp = "^\\+?[0-9 ]{5,20}$",
            message = "Phone number may contain only digits, spaces and a leading +"
    )
    private String phoneNumber;

    @Size(min = 1, max = 20, message = "Gender must be between 1 and 20 characters")
    private String gender;

    public RegisterRequest() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName.strip();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName.strip();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email.strip().toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddressLine1(String address) {
        this.address = address == null ? null : address.strip();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city == null ? null : city.strip();
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode == null ? null : postcode.strip();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.strip();
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender == null ? null : gender.strip().toUpperCase();
    }
}
