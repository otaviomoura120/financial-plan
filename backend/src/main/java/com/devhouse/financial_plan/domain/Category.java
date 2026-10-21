package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.CategoryKind;
import com.devhouse.financial_plan.domain.enums.SystemCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class Category {

    private Long id;
    private Integer version;
    private Space space;
    private String name;
    private CategoryKind kind;
    private boolean active;
    private final Instant createdDate;
    private Instant updatedDate;

    public Category(Long id, Integer version, Space space, String name, CategoryKind kind, boolean active,
                    Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.space = space;
        this.name = name;
        this.kind = kind;
        this.active = active;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (space == null) {
            throw new DomainException("Category must belong to a space");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Category name cannot be empty");
        }
        if (kind == null && !isSystem()) {
            throw new DomainException("Category type is required");
        }
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Category name cannot be empty");
        }
        this.name = name;
        this.updatedDate = Instant.now();
    }

    public void changeKind(CategoryKind kind) {
        if (kind == null) {
            throw new DomainException("Category type is required");
        }
        this.kind = kind;
        this.updatedDate = Instant.now();
    }

    public boolean isKindChangedTo(CategoryKind otherKind) {
        return otherKind != null && !otherKind.equals(kind);
    }

    public void requireKind(CategoryKind expected) {
        if (!Objects.equals(expected, kind)) {
            throw new DomainException(CategoryKind.INCOME.equals(expected)
                    ? "The selected category is not an income category"
                    : "The selected category is not an expense category");
        }
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public boolean isSystem() {
        return SystemCategory.fromCategoryName(name) != null;
    }

    public boolean hasSameName(String otherName) {
        if (name == null || otherName == null) {
            return false;
        }
        return name.trim().equalsIgnoreCase(otherName.trim());
    }

    public boolean belongsToSpace(Long spaceId) {
        return space != null && Objects.equals(space.getId(), spaceId);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking category", new Exception());
        }
        this.version = version;
    }

    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoryKind getKind() { return kind; }
    public void setKind(CategoryKind kind) { this.kind = kind; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
