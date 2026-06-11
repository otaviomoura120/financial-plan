package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class PaymentMethod {

    private Long id;
    private Integer version;
    private String name;
    private boolean active;
    private final Instant createdDate;
    private Instant updatedDate;

    public PaymentMethod(Long id, Integer version, String name, boolean active, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.active = active;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new DomainException("Payment method name cannot be empty");
        }
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Payment method name cannot be empty");
        }
        this.name = name;
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
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking paymentMethod", new Exception());
        }
        this.version = version;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
