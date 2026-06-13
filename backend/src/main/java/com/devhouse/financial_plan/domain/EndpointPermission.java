package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public class EndpointPermission {

    private Long id;
    private Integer version;
    private String endpoint;
    private String name;
    private String icon;
    private Integer sequence;
    private EndpointPermissionType type;
    private String permittedMethods;
    private String permittedRoles;
    private final Instant createdAt;
    private Instant updatedAt;

    public EndpointPermission(Long id, Integer version, String endpoint, String name, String icon,
                              Integer sequence, EndpointPermissionType type,
                              String permittedMethods, String permittedRoles, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.version = version;
        this.endpoint = endpoint;
        this.name = name;
        this.icon = icon;
        this.sequence = sequence;
        this.type = type;
        this.permittedMethods = permittedMethods;
        this.permittedRoles = permittedRoles;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (endpoint == null || endpoint.isBlank()) {
            throw new DomainException("EndpointPermission endpoint cannot be empty");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("EndpointPermission name cannot be empty");
        }
        if (type == null) {
            throw new DomainException("EndpointPermission type cannot be null");
        }
    }

    public boolean matchesRequest(String method, String path) {
        boolean methodMatches = Arrays.stream(permittedMethods.split(","))
                .map(String::trim)
                .anyMatch(m -> m.equalsIgnoreCase(method));
        if (!methodMatches) {
            return false;
        }
        return path.matches(endpoint);
    }

    public boolean isPermitted(String roleName) {
        return Arrays.stream(permittedRoles.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equals(roleName));
    }

    public void update(String endpoint, String name, String icon, Integer sequence,
                       EndpointPermissionType type, String permittedMethods, String permittedRoles) {
        this.endpoint = endpoint;
        this.name = name;
        this.icon = icon;
        this.sequence = sequence;
        this.type = type;
        this.permittedMethods = permittedMethods;
        this.permittedRoles = permittedRoles;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking endpoint permission", new Exception());
        }
        this.version = version;
    }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public EndpointPermissionType getType() { return type; }
    public void setType(EndpointPermissionType type) { this.type = type; }
    public String getPermittedMethods() { return permittedMethods; }
    public void setPermittedMethods(String permittedMethods) { this.permittedMethods = permittedMethods; }
    public String getPermittedRoles() { return permittedRoles; }
    public void setPermittedRoles(String permittedRoles) { this.permittedRoles = permittedRoles; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
