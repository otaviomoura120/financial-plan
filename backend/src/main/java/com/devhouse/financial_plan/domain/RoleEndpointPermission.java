package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess;
import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class RoleEndpointPermission {

    private Long id;
    private Integer version;
    private Role role;
    private EndpointPermission endpointPermission;
    private EndpointPermissionAccess permission;
    private final Instant createdAt;
    private Instant updatedAt;

    public RoleEndpointPermission(Long id, Integer version, Role role, EndpointPermission endpointPermission,
                                  EndpointPermissionAccess permission, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.version = version;
        this.role = role;
        this.endpointPermission = endpointPermission;
        this.permission = permission;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (role == null) {
            throw new DomainException("RoleEndpointPermission role cannot be null");
        }
        if (endpointPermission == null) {
            throw new DomainException("RoleEndpointPermission endpointPermission cannot be null");
        }
        if (permission == null) {
            throw new DomainException("RoleEndpointPermission permission cannot be null");
        }
    }

    public void allow() {
        this.permission = EndpointPermissionAccess.ALLOW;
        this.updatedAt = Instant.now();
    }

    public void deny() {
        this.permission = EndpointPermissionAccess.DENY;
        this.updatedAt = Instant.now();
    }

    public boolean isAllowed() {
        return permission == EndpointPermissionAccess.ALLOW;
    }

    public Long getId() { return id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking role endpoint permission", new Exception());
        }
        this.version = version;
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public EndpointPermission getEndpointPermission() { return endpointPermission; }
    public void setEndpointPermission(EndpointPermission endpointPermission) { this.endpointPermission = endpointPermission; }
    public EndpointPermissionAccess getPermission() { return permission; }
    public void setPermission(EndpointPermissionAccess permission) { this.permission = permission; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
