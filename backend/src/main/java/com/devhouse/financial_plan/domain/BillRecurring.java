package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private LocalDate endDate;
    private Integer installments;
    private boolean active;
    private final Instant createdDate;
    private Instant updatedDate;

    public BillRecurring(Long id, Integer version, Space space, String name, Category category, SubCategory subCategory,
                          BigDecimal defaultAmount, LocalDate startDate, LocalDate endDate, Integer installments,
                          boolean active, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.space = space;
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.defaultAmount = defaultAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.installments = installments;
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
        validateEnd();
    }

    private void validateEnd() {
        if (endDate != null && installments != null) {
            throw new DomainException("Bill recurring cannot define both end date and installments");
        }
        if (installments != null && installments <= 0) {
            throw new DomainException("Bill recurring installments must be positive");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new DomainException("Bill recurring end date cannot be before start date");
        }
    }

    /**
     * Last month that must be generated, or null when the recurrence never ends.
     */
    public YearMonth lastReferenceMonth() {
        if (installments != null) {
            return YearMonth.from(startDate).plusMonths(installments - 1L);
        }
        if (endDate != null) {
            return YearMonth.from(endDate);
        }
        return null;
    }

    public boolean isFinishedOn(YearMonth month) {
        YearMonth lastMonth = lastReferenceMonth();
        return lastMonth != null && month.isAfter(lastMonth);
    }

    public void update(String name, Category category, SubCategory subCategory, BigDecimal defaultAmount) {
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.defaultAmount = defaultAmount;
        this.updatedDate = Instant.now();
    }

    public void updateSchedule(LocalDate startDate, LocalDate endDate, Integer installments) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.installments = installments;
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
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
