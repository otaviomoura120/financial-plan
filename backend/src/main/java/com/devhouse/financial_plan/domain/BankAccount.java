package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class BankAccount {

    private Long id;
    private Integer version;
    private Space space;
    private String name;
    private String bankName;
    private BigDecimal balance;
    private boolean active;
    private final Instant createdDate;
    private Instant updatedDate;

    public BankAccount(Long id, Integer version, Space space, String name, String bankName, BigDecimal balance, boolean active, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.space = space;
        this.name = name;
        this.bankName = bankName;
        this.balance = balance;
        this.active = active;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new DomainException("Bank account name cannot be empty");
        }
        if (space == null) {
            throw new DomainException("Bank account must be associated with a space");
        }
        if (balance == null) {
            throw new DomainException("Bank account balance cannot be null");
        }
    }

    public void update(String name, String bankName) {
        this.name = name;
        this.bankName = bankName;
        this.updatedDate = Instant.now();
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Debit amount must be positive");
        }
        this.balance = this.balance.subtract(amount);
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
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking bankAccount", new Exception());
        }
        this.version = version;
    }

    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
