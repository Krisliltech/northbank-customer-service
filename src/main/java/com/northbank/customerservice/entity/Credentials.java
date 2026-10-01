package com.northbank.customerservice.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "credentials")
public class Credentials {
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
    private String role = "CUSTOMER";

    public Credentials() {
    }

    public Credentials(UUID customerId, Customer customer, String password) {
        this.customerId = customerId;
        this.customer = customer;
        this.password = password;
    }

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
        this.role = role;
    }
}
