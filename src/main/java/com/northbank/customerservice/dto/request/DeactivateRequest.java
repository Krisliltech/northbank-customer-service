package com.northbank.customerservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public class DeactivateRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    public DeactivateRequest() {}

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
