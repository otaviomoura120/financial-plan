package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class Role {

    public static final String OWNER_ROLE_NAME = "OWNER";

    private Long id;
    private Integer version;
    private Space space;
    private String name;
    private String description;
    private final Instant createdAt;
    private Instant updatedAt;

    public Role(Long id, Integer version, Space space, String name, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.version = version;
        this.space = space;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (space == null) {
            throw new DomainException("Role space cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Role name cannot be empty");
        }
    }

    /**
     * The owner is recognised by the role name (SpaceMember.isOwner), so only CreateSpaceService may create a role
     * with it. The check ignores case and spaces so a look-alike such as " owner" cannot pass for the owner role.
     */
    public static void requireNotReservedName(String name) {
        if (name != null && OWNER_ROLE_NAME.equalsIgnoreCase(name.trim())) {
            throw new DomainException("Role name " + OWNER_ROLE_NAME + " is reserved for the space owner");
        }
    }

    public boolean belongsToSpace(Long spaceId) {
        return space != null && Objects.equals(space.getId(), spaceId);
    }

    public boolean isOwnerRole() {
        return OWNER_ROLE_NAME.equals(name);
    }

    public void update(String name, String description) {
        if (isOwnerRole()) {
            if (!OWNER_ROLE_NAME.equals(name)) {
                throw new DomainException("The space owner role cannot be renamed");
            }
        } else {
            requireNotReservedName(name);
        }
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

    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
