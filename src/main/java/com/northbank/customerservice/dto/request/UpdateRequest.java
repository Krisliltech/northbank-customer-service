package com.northbank.customerservice.dto.request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateRequest {
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

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

    public UpdateRequest() {}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName == null ? null : firstName.strip();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName ==  null ? null : lastName.strip();
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

    public void setAddress(String address) {
        this.address = address == null ? null : address.strip();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = (city == null) ? null : city.strip();
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
