package com.devhouse.financial_plan.infrastructure.repository.jpa;

import com.devhouse.financial_plan.domain.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class TransactionEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntityJpa user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccountEntityJpa bankAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_bank_account_id")
    private BankAccountEntityJpa destinationBankAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntityJpa category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategoryEntityJpa subCategory;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethodEntityJpa paymentMethod;
    private BigDecimal amount;
    @Column(name = "transaction_date")
    private LocalDate transactionDate;
    private String description;
    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public UserEntityJpa getUser() { return user; }
    public void setUser(UserEntityJpa user) { this.user = user; }
    public BankAccountEntityJpa getBankAccount() { return bankAccount; }
    public void setBankAccount(BankAccountEntityJpa bankAccount) { this.bankAccount = bankAccount; }
    public BankAccountEntityJpa getDestinationBankAccount() { return destinationBankAccount; }
    public void setDestinationBankAccount(BankAccountEntityJpa destinationBankAccount) { this.destinationBankAccount = destinationBankAccount; }
    public CategoryEntityJpa getCategory() { return category; }
    public void setCategory(CategoryEntityJpa category) { this.category = category; }
    public SubCategoryEntityJpa getSubCategory() { return subCategory; }
    public void setSubCategory(SubCategoryEntityJpa subCategory) { this.subCategory = subCategory; }
    public PaymentMethodEntityJpa getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethodEntityJpa paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
