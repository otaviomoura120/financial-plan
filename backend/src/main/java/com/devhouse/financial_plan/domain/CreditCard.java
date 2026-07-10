package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class CreditCard {

    private Long id;
    private Integer version;
    private Space space;
    private BankAccount bankAccount;
    private String name;
    private BigDecimal limit;
    private Integer closingDay;
    private Integer dueDay;
    private boolean active;
    private final Instant createdDate;
    private Instant updatedDate;

    public CreditCard(Long id, Integer version, Space space, BankAccount bankAccount, String name, BigDecimal limit, Integer closingDay, Integer dueDay, boolean active, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.space = space;
        this.bankAccount = bankAccount;
        this.name = name;
        this.limit = limit;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
        this.active = active;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new DomainException("Credit card name cannot be empty");
        }
        if (space == null) {
            throw new DomainException("Credit card must be associated with a space");
        }
        if (limit == null || limit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Credit card limit must be positive");
        }
        validateDay(closingDay, "Closing day");
        validateDay(dueDay, "Due day");
    }

    private void validateDay(Integer day, String fieldName) {
        if (day == null || day < 1 || day > 31) {
            throw new DomainException(fieldName + " must be between 1 and 31");
        }
    }

    public void update(String name, BigDecimal limit, Integer closingDay, Integer dueDay, BankAccount bankAccount) {
        this.name = name;
        this.limit = limit;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
        this.bankAccount = bankAccount;
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
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking creditCard", new Exception());
        }
        this.version = version;
    }

    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public BankAccount getBankAccount() { return bankAccount; }
    public void setBankAccount(BankAccount bankAccount) { this.bankAccount = bankAccount; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getLimit() { return limit; }
    public void setLimit(BigDecimal limit) { this.limit = limit; }
    public Integer getClosingDay() { return closingDay; }
    public void setClosingDay(Integer closingDay) { this.closingDay = closingDay; }
    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
