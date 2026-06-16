package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.InviteStatus;
import com.devhouse.financial_plan.domain.exception.DomainException;

import java.time.Instant;

public class SpaceInvite {

    private Long id;
    private Space space;
    private Role role;
    private String email;
    private String token;
    private InviteStatus status;
    private final Instant createdAt;
    private Instant expiresAt;

    public SpaceInvite(Long id, Space space, Role role, String email, String token,
                       InviteStatus status, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.space = space;
        this.role = role;
        this.email = email;
        this.token = token;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void validate() {
        if (space == null) throw new DomainException("SpaceInvite must belong to a space");
        if (role == null) throw new DomainException("SpaceInvite must have a role");
        if (email == null || email.isBlank()) throw new DomainException("SpaceInvite email cannot be empty");
        if (token == null || token.isBlank()) throw new DomainException("SpaceInvite token cannot be empty");
    }

    public void cancel() {
        this.status = InviteStatus.CANCELLED;
    }

    public void accept() {
        this.status = InviteStatus.ACCEPTED;
    }

    public void decline() {
        this.status = InviteStatus.DECLINED;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Space getSpace() { return space; }
    public Role getRole() { return role; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
    public InviteStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
