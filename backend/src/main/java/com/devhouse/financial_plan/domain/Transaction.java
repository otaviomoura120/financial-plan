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
    private User user;
    private BankAccount bankAccount;
    private BankAccount destinationBankAccount;
    private Category category;
    private SubCategory subCategory;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String description;
    private final Instant createdDate;
    private Instant updatedDate;

    public Transaction(Long id, Integer version, TransactionType type, User user, BankAccount bankAccount, BankAccount destinationBankAccount, Category category, SubCategory subCategory, PaymentMethod paymentMethod, BigDecimal amount, LocalDate transactionDate, String description, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.type = type;
        this.user = user;
        this.bankAccount = bankAccount;
        this.destinationBankAccount = destinationBankAccount;
        this.category = category;
        this.subCategory = subCategory;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (type == null) {
            throw new DomainException("Transaction type is required");
        }
        if (user == null) {
            throw new DomainException("User is required");
        }
        if (bankAccount == null) {
            throw new DomainException("Bank account is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Amount must be positive");
        }
        if (transactionDate == null) {
            throw new DomainException("Transaction date is required");
        }
        if (isTransfer()) {
            if (destinationBankAccount == null) {
                throw new DomainException("Destination bank account is required for a transfer");
            }
            if (destinationBankAccount.getId().equals(bankAccount.getId())) {
                throw new DomainException("Destination bank account must be different from the origin bank account");
            }
        } else {
            if (category == null) {
                throw new DomainException("Category is required");
            }
            if (paymentMethod == null) {
                throw new DomainException("Payment method is required");
            }
        }
    }

    public void update(TransactionType type, BankAccount bankAccount, BankAccount destinationBankAccount, Category category, SubCategory subCategory,
                       PaymentMethod paymentMethod, BigDecimal amount, LocalDate transactionDate, String description) {
        this.type = type;
        this.bankAccount = bankAccount;
        this.destinationBankAccount = destinationBankAccount;
        this.category = category;
        this.subCategory = subCategory;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.updatedDate = Instant.now();
    }

    public boolean isIncome() {
        return TransactionType.INCOME.equals(type);
    }

    public boolean isExpense() {
        return TransactionType.EXPENSE.equals(type);
    }

    public boolean isTransfer() {
        return TransactionType.TRANSFER.equals(type);
    }

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
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BankAccount getBankAccount() { return bankAccount; }
    public void setBankAccount(BankAccount bankAccount) { this.bankAccount = bankAccount; }
    public BankAccount getDestinationBankAccount() { return destinationBankAccount; }
    public void setDestinationBankAccount(BankAccount destinationBankAccount) { this.destinationBankAccount = destinationBankAccount; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public SubCategory getSubCategory() { return subCategory; }
    public void setSubCategory(SubCategory subCategory) { this.subCategory = subCategory; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
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
