package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class SpaceMember {

    private Long id;
    private Integer version;
    private Space space;
    private User user;
    private Role role;
    private final Instant joinedAt;

    public SpaceMember(Long id, Integer version, Space space, User user, Role role, Instant joinedAt) {
        this.id = id;
        this.version = version;
        this.space = space;
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public void validate() {
        if (space == null) {
            throw new DomainException("SpaceMember must belong to a space");
        }
        if (user == null) {
            throw new DomainException("SpaceMember must be associated with a user");
        }
        if (role == null) {
            throw new DomainException("SpaceMember must have a role");
        }
    }

    public void assignRole(Role newRole) {
        this.role = newRole;
    }

    public boolean isOwner() {
        return role != null && Role.OWNER_ROLE_NAME.equals(role.getName());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking space member", new Exception());
        }
        this.version = version;
    }

    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
}
