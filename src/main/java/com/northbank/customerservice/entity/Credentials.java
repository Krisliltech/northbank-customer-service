package com.northbank.customerservice.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "credentials")
public class Credentials implements Persistable<UUID> {
    @Id
    @Column(name = "customer_id")
    private UUID customerId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;

    @Transient
    private boolean isNew = true;

    public Credentials() {
    }

    public Credentials(UUID customerId, Customer customer, String password, String role) {
        this.customerId = customerId;
        this.customer = customer;
        this.password = password;
        this.role = (role != null && !role.isBlank()) ? role : "CUSTOMER";
    }

    @Override
    public UUID getId() { return customerId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    public UUID getCustomerId() {
        return customerId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(String role) {
        this.role = (role != null && !role.isBlank()) ? role : "CUSTOMER";
    }
}
