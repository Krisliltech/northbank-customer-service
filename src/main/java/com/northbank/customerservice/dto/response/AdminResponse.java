package com.northbank.customerservice.dto.response;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class AdminResponse extends CustomerResponse {
    private String role;

    public AdminResponse(CustomerResponse base, String role) {
        super(base.getId(), base.getFirstName(), base.getLastName(), base.getEmail(),
                base.getDateOfBirth(), base.getStatus(), base.getPhoneNumber(),
                base.getAddress(), base.getCity(), base.getGender(), base.getPostCode());
        this.role = role;
    }

}
