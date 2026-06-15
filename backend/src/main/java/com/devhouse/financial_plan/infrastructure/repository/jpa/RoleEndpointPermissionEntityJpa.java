package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "role_endpoint_permissions")
public class RoleEndpointPermissionEntityJpa {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private RoleEntityJpa role;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_permission_id")
    private EndpointPermissionEntityJpa endpointPermission;
    private String permission;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public RoleEntityJpa getRole() { return role; }
    public void setRole(RoleEntityJpa role) { this.role = role; }
    public EndpointPermissionEntityJpa getEndpointPermission() { return endpointPermission; }
    public void setEndpointPermission(EndpointPermissionEntityJpa endpointPermission) { this.endpointPermission = endpointPermission; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
