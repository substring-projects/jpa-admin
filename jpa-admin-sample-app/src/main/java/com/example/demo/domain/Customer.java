package com.example.demo.domain;

import io.github.jpaadmin.annotations.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@AdminResource(
    displayName = "Customer",
    pluralName = "Customers",
    group = "Users",
    icon = "users"
)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @AdminField(label = "ID", order = 1, sortable = true)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    @AdminField(label = "Full Name", order = 2, searchable = true, sortable = true)
    private String fullName;

    @Column(nullable = false, unique = true)
    @Email
    @AdminField(label = "Email Address", order = 3, searchable = true, sortable = true)
    private String email;

    @AdminField(label = "Phone Number", order = 4)
    private String phone;

    @AdminField(label = "Password", order = 5, inList = false)
    @AdminSensitive
    private String password;

    @AdminField(label = "VIP Member", order = 6, filterable = true)
    private boolean vip = false;

    @Column(updatable = false)
    @AdminField(label = "Registered Date", order = 7, readOnly = true, inForm = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    public Customer() {}

    public Customer(String fullName, String email, String phone, String password, boolean vip) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.vip = vip;
        this.registeredAt = LocalDateTime.now();
    }

    @AdminAction(name = "Upgrade to VIP", icon = "star", scope = ActionScope.BOTH)
    public void upgrade() {
        this.vip = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isVip() { return vip; }
    public void setVip(boolean vip) { this.vip = vip; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
