package com.northbank.customerservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    @Pattern(regexp = "^\\S(.*\\S)?$", message = "Password must not start or end with whitespace")
    private String newPassword;

    public ChangePasswordRequest() {}

    public String getCurrentPassword() { return currentPassword; }

    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }

    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
