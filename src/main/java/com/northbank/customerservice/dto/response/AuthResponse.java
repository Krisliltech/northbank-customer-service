package com.northbank.customerservice.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponse {
    private String token;
    public UUID id;
    private String firstName;
    private String lastName;
    private String email;

    public AuthResponse(String token, UUID id, String firstName, String lastName, String email) {
        this.token = token;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
