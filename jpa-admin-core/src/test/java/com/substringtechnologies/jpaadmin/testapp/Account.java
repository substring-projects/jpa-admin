package com.substringtechnologies.jpaadmin.testapp;

import com.substringtechnologies.jpaadmin.annotations.*;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@AdminResource(pageSize = 15)
public class Account {
    @Id @GeneratedValue
    private Long id;
    private String username;

    @AdminSensitive
    private String password;

    @AdminSensitive(mask = false)
    private String apiKey;

    @AdminSensitive(maskPattern = "[set]")
    private String recoveryCode;

    @AdminWidget(type = WidgetType.SLIDER, min = 0, max = 5, step = 0.5)
    private Double rating;

    @AdminWidget(min = 10)
    private Integer seats;

    @AdminWidget(type = WidgetType.CODE_EDITOR, language = "SQL")
    private String query;

    @AdminWidget(type = WidgetType.IMAGE_URL, previewSize = "64px")
    private String avatarUrl;

    @ManyToOne
    @AdminLookup(lookupProperty = "name", searchProperty = "code")
    private Team team;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @AdminInline(type = InlineType.STACKED)
    private List<Credential> credentials = new ArrayList<>();

    @Transient
    @AdminComputed(format = "%.1f%%")
    public Double getScore() {
        return rating == null ? null : rating * 20;
    }

    @AdminAction(name = "Suspend", icon = "ban")
    public void suspend() {
        throw new IllegalStateException("Accounts on a team can't be suspended");
    }

    @AdminAction(name = "Crash")
    public void crash() {
        throw new NullPointerException("internal detail: table ACCOUNT_SECRET");
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getRecoveryCode() { return recoveryCode; }
    public void setRecoveryCode(String recoveryCode) { this.recoveryCode = recoveryCode; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public List<Credential> getCredentials() { return credentials; }
    public void setCredentials(List<Credential> credentials) { this.credentials = credentials; }
}
