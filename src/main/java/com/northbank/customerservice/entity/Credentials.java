package com.northbank.customerservice.entity;

import jakarta.persistence.*;
import org.jspecify.annotations.NonNull;
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

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Transient
    private boolean isNew = true;

    public Credentials() {
    }

    public Credentials(UUID customerId, Customer customer, String password) {
        this.customerId = customerId;
        this.customer = customer;
        this.password = password;
    }

    @Override
    public @NonNull UUID getId() { return customerId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    public Customer getCustomer() {
        return customer;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.CUSTOMER;
    }
}
