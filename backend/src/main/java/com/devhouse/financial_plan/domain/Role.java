package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class Role {

    public static final String OWNER_ROLE_NAME = "OWNER";

    private Long id;
    private Integer version;
    private Family family;
    private String name;
    private String description;
    private final Instant createdAt;
    private Instant updatedAt;

    public Role(Long id, Integer version, Family family, String name, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.version = version;
        this.family = family;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (family == null) {
            throw new DomainException("Role family cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Role name cannot be empty");
        }
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking role", new Exception());
        }
        this.version = version;
    }

    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
