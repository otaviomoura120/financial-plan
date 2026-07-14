package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "credit_card_transaction_recurrings")
public class CreditCardTransactionRecurringEntityJpa {

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
    private String description;
    @Column(name = "default_amount")
    private BigDecimal defaultAmount;
    @Column(name = "start_date")
    private LocalDate startDate;
    private boolean active;
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
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
