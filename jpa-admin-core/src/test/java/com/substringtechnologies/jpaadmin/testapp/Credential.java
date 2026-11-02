package com.substringtechnologies.jpaadmin.testapp;

import com.substringtechnologies.jpaadmin.annotations.AdminSensitive;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Credential {
    @Id @GeneratedValue
    private Long id;
    private String label;

    @AdminSensitive
    private String secret;

    @ManyToOne
    private Account account;

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}
