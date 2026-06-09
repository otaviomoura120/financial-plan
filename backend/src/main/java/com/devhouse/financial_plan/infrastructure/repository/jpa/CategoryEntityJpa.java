package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class CategoryEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private boolean active;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
