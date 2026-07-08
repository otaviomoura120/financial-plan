package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class BillRecurring {

    private Long id;
    private Integer version;
    private Space space;
    private String name;
    private Category category;
    private SubCategory subCategory;
    private BigDecimal defaultAmount;
    private LocalDate startDate;
    private boolean active;
    private final Instant createdDate;
    private Instant updatedDate;

    public BillRecurring(Long id, Integer version, Space space, String name, Category category, SubCategory subCategory,
                          BigDecimal defaultAmount, LocalDate startDate, boolean active, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.space = space;
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.defaultAmount = defaultAmount;
        this.startDate = startDate;
        this.active = active;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new DomainException("Bill recurring name cannot be empty");
        }
        if (space == null) {
            throw new DomainException("Bill recurring must be associated with a space");
        }
        if (defaultAmount == null || defaultAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Bill recurring default amount must be positive");
        }
        if (startDate == null) {
            throw new DomainException("Bill recurring start date is required");
        }
    }

    public void update(String name, Category category, SubCategory subCategory, BigDecimal defaultAmount) {
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.defaultAmount = defaultAmount;
        this.updatedDate = Instant.now();
    }

    public void updateSchedule(LocalDate startDate) {
        this.startDate = startDate;
        this.updatedDate = Instant.now();
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking billRecurring", new Exception());
        }
        this.version = version;
    }

    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public SubCategory getSubCategory() { return subCategory; }
    public void setSubCategory(SubCategory subCategory) { this.subCategory = subCategory; }
    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
