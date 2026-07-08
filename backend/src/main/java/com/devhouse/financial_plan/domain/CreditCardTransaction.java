package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class CreditCardTransaction {

    private Long id;
    private Integer version;
    private CreditCard creditCard;
    private User user;
    private Category category;
    private SubCategory subCategory;
    private BigDecimal amount;
    private LocalDate purchaseDate;
    private String description;
    private LocalDate referenceMonth;
    private String installmentGroupId;
    private Integer installmentNumber;
    private Integer totalInstallments;
    private boolean anticipated;
    private LocalDate originalReferenceMonth;
    private final Instant createdDate;
    private Instant updatedDate;

    public CreditCardTransaction(Long id, Integer version, CreditCard creditCard, User user, Category category, SubCategory subCategory,
                                  BigDecimal amount, LocalDate purchaseDate, String description, LocalDate referenceMonth,
                                  String installmentGroupId, Integer installmentNumber, Integer totalInstallments,
                                  boolean anticipated, LocalDate originalReferenceMonth, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.creditCard = creditCard;
        this.user = user;
        this.category = category;
        this.subCategory = subCategory;
        this.amount = amount;
        this.purchaseDate = purchaseDate;
        this.description = description;
        this.referenceMonth = referenceMonth;
        this.installmentGroupId = installmentGroupId;
        this.installmentNumber = installmentNumber;
        this.totalInstallments = totalInstallments;
        this.anticipated = anticipated;
        this.originalReferenceMonth = originalReferenceMonth;
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
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Amount must be positive");
        }
        if (purchaseDate == null) {
            throw new DomainException("Purchase date is required");
        }
        if (referenceMonth == null) {
            throw new DomainException("Reference month is required");
        }
        if (installmentGroupId == null || installmentGroupId.isBlank()) {
            throw new DomainException("Installment group id is required");
        }
        if (totalInstallments == null || totalInstallments < 1 || totalInstallments > 60) {
            throw new DomainException("Total installments must be between 1 and 60");
        }
        if (installmentNumber == null || installmentNumber < 1 || installmentNumber > totalInstallments) {
            throw new DomainException("Installment number must be between 1 and the total number of installments");
        }
    }

    public void anticipateTo(LocalDate targetReferenceMonth) {
        if (!anticipated) {
            this.originalReferenceMonth = this.referenceMonth;
        }
        this.referenceMonth = targetReferenceMonth;
        this.anticipated = true;
        this.updatedDate = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking creditCardTransaction", new Exception());
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
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getReferenceMonth() { return referenceMonth; }
    public void setReferenceMonth(LocalDate referenceMonth) { this.referenceMonth = referenceMonth; }
    public String getInstallmentGroupId() { return installmentGroupId; }
    public void setInstallmentGroupId(String installmentGroupId) { this.installmentGroupId = installmentGroupId; }
    public Integer getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }
    public Integer getTotalInstallments() { return totalInstallments; }
    public void setTotalInstallments(Integer totalInstallments) { this.totalInstallments = totalInstallments; }
    public boolean isAnticipated() { return anticipated; }
    public void setAnticipated(boolean anticipated) { this.anticipated = anticipated; }
    public LocalDate getOriginalReferenceMonth() { return originalReferenceMonth; }
    public void setOriginalReferenceMonth(LocalDate originalReferenceMonth) { this.originalReferenceMonth = originalReferenceMonth; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
