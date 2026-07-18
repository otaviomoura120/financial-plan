package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bill_recurrings")
public class BillRecurringEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private SpaceEntityJpa space;
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntityJpa category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private SubCategoryEntityJpa subCategory;
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
    public SpaceEntityJpa getSpace() { return space; }
    public void setSpace(SpaceEntityJpa space) { this.space = space; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoryEntityJpa getCategory() { return category; }
    public void setCategory(CategoryEntityJpa category) { this.category = category; }
    public SubCategoryEntityJpa getSubCategory() { return subCategory; }
    public void setSubCategory(SubCategoryEntityJpa subCategory) { this.subCategory = subCategory; }
    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
