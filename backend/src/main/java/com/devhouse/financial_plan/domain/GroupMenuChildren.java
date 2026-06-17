package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class GroupMenuChildren {

    private Long id;
    private Integer version;
    private String name;
    private String endpoint;
    private String icon;
    private GroupMenu groupMenu;
    private final Instant createdAt;
    private Instant updatedAt;

    public GroupMenuChildren(Long id, Integer version, String name, String endpoint, String icon, GroupMenu groupMenu, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.endpoint = endpoint;
        this.icon = icon;
        this.groupMenu = groupMenu;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new DomainException("GroupMenuChildren name cannot be empty");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new DomainException("GroupMenuChildren endpoint cannot be empty");
        }
    }

    public void update(String name, String endpoint, String icon) {
        this.name = name;
        this.endpoint = endpoint;
        this.icon = icon;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking group menu children", new Exception());
        }
        this.version = version;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public GroupMenu getGroupMenu() { return groupMenu; }
    public void setGroupMenu(GroupMenu groupMenu) { this.groupMenu = groupMenu; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
