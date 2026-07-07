package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class SubCategory {

    private Long id;
    private Integer version;
    private Category category;
    private String name;
    private boolean active;
    private final Instant createdDate;
    private Instant updatedDate;

    public SubCategory(Long id, Integer version, Category category, String name, boolean active, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.category = category;
        this.name = name;
        this.active = active;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new DomainException("SubCategory name cannot be empty");
        }
        if (category == null) {
            throw new DomainException("SubCategory must belong to a category");
        }
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("SubCategory name cannot be empty");
        }
        this.name = name;
        this.updatedDate = Instant.now();
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking subCategory", new Exception());
        }
        this.version = version;
    }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
