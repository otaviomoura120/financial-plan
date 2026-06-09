package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "sub_categories")
public class SubCategoryEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "category_id")
    private Long categoryId;
    private String name;
    private boolean active;

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
