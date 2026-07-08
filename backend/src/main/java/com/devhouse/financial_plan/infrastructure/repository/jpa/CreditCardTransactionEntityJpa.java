package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "credit_card_transactions",
        indexes = @Index(name = "idx_credit_card_transactions_card_reference_month", columnList = "credit_card_id, reference_month"))
public class CreditCardTransactionEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCardEntityJpa creditCard;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntityJpa user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntityJpa category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategoryEntityJpa subCategory;
    private BigDecimal amount;
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
    private String description;
    @Column(name = "reference_month")
    private LocalDate referenceMonth;
    @Column(name = "installment_group_id")
    private String installmentGroupId;
    @Column(name = "installment_number")
    private Integer installmentNumber;
    @Column(name = "total_installments")
    private Integer totalInstallments;
    private boolean anticipated;
    @Column(name = "original_reference_month")
    private LocalDate originalReferenceMonth;
    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public CreditCardEntityJpa getCreditCard() { return creditCard; }
    public void setCreditCard(CreditCardEntityJpa creditCard) { this.creditCard = creditCard; }
    public UserEntityJpa getUser() { return user; }
    public void setUser(UserEntityJpa user) { this.user = user; }
    public CategoryEntityJpa getCategory() { return category; }
    public void setCategory(CategoryEntityJpa category) { this.category = category; }
    public SubCategoryEntityJpa getSubCategory() { return subCategory; }
    public void setSubCategory(SubCategoryEntityJpa subCategory) { this.subCategory = subCategory; }
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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
