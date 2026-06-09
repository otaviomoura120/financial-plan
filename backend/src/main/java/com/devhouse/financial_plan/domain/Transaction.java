package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class Transaction {

    private Long id;
    private Integer version;
    private TransactionType type;
    private Long userId;
    private Long bankAccountId;
    private Long categoryId;
    private Long subCategoryId;
    private Long paymentMethodId;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String description;
    private final Instant createdDate;
    private Instant updatedDate;

    public Transaction(Long id, Integer version, TransactionType type, Long userId, Long bankAccountId, Long categoryId, Long subCategoryId, Long paymentMethodId, BigDecimal amount, LocalDate transactionDate, String description, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.type = type;
        this.userId = userId;
        this.bankAccountId = bankAccountId;
        this.categoryId = categoryId;
        this.subCategoryId = subCategoryId;
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (type == null) throw new DomainException("Transaction type is required");
        if (userId == null) throw new DomainException("User is required");
        if (bankAccountId == null) throw new DomainException("Bank account is required");
        if (categoryId == null) throw new DomainException("Category is required");
        if (paymentMethodId == null) throw new DomainException("Payment method is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new DomainException("Amount must be positive");
        if (transactionDate == null) throw new DomainException("Transaction date is required");
    }

    public boolean isIncome() { return TransactionType.INCOME.equals(type); }
    public boolean isExpense() { return TransactionType.EXPENSE.equals(type); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking transaction", new Exception());
        }
        this.version = version;
    }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getSubCategoryId() { return subCategoryId; }
    public void setSubCategoryId(Long subCategoryId) { this.subCategoryId = subCategoryId; }
    public Long getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
