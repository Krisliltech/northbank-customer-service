package com.northbank.customerservice.dto.request;

import com.northbank.customerservice.entity.Status;
import jakarta.validation.constraints.NotNull;

public class StatusRequest {
    @NotNull(message = "Status is required")
    private Status status;

    public StatusRequest() {}

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) { this.status = status; }
}
