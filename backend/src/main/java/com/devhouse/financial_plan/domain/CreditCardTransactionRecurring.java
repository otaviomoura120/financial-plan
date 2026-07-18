package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class CreditCardTransactionRecurring {

    private Long id;
    private Integer version;
    private CreditCard creditCard;
    private User user;
    private Category category;
    private SubCategory subCategory;
    private String description;
    private BigDecimal defaultAmount;
    private LocalDate startDate;
    private boolean active;
    private final Instant createdDate;
    private Instant updatedDate;

    public CreditCardTransactionRecurring(Long id, Integer version, CreditCard creditCard, User user, Category category,
                                           SubCategory subCategory, String description, BigDecimal defaultAmount,
                                           LocalDate startDate, boolean active, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.creditCard = creditCard;
        this.user = user;
        this.category = category;
        this.subCategory = subCategory;
        this.description = description;
        this.defaultAmount = defaultAmount;
        this.startDate = startDate;
        this.active = active;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (creditCard == null) {
            throw new DomainException("Credit card is required");
        }
        if (user == null) {
            throw new DomainException("User is required");
        }
        if (category == null) {
            throw new DomainException("Category is required");
        }
        if (defaultAmount == null || defaultAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Default amount must be positive");
        }
        if (startDate == null) {
            throw new DomainException("Start date is required");
        }
    }

    public void update(Category category, SubCategory subCategory, BigDecimal defaultAmount, String description) {
        this.category = category;
        this.subCategory = subCategory;
        this.defaultAmount = defaultAmount;
        this.description = description;
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
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking creditCardTransactionRecurring", new Exception());
        }
        this.version = version;
    }

    public CreditCard getCreditCard() { return creditCard; }
    public void setCreditCard(CreditCard creditCard) { this.creditCard = creditCard; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public SubCategory getSubCategory() { return subCategory; }
    public void setSubCategory(SubCategory subCategory) { this.subCategory = subCategory; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
