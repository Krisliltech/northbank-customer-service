package com.northbank.customerservice.dto.request;

import com.northbank.customerservice.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoleRequest {
    @NotNull(message = "Role is required")
    private Role role;

}
