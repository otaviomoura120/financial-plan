package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;

import java.time.Instant;
import java.util.List;

public class GroupMenu {

    private Long id;
    private String name;
    private String icon;
    private List<GroupMenuChildren> children;
    private final Instant createdAt;
    private Instant updatedAt;

    public GroupMenu(Long id, String name, String icon, List<GroupMenuChildren> children, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.children = children;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new DomainException("GroupMenu name cannot be empty");
        }
    }

    public void update(String name, String icon) {
        this.name = name;
        this.icon = icon;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public List<GroupMenuChildren> getChildren() { return children; }
    public void setChildren(List<GroupMenuChildren> children) { this.children = children; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
