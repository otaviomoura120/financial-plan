package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "credit_cards")
public class CreditCardEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private SpaceEntityJpa space;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccountEntityJpa bankAccount;
    private String name;
    @Column(name = "credit_limit")
    private BigDecimal limit;
    @Column(name = "closing_day")
    private Integer closingDay;
    @Column(name = "due_day")
    private Integer dueDay;
    private boolean active;
    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public SpaceEntityJpa getSpace() { return space; }
    public void setSpace(SpaceEntityJpa space) { this.space = space; }
    public BankAccountEntityJpa getBankAccount() { return bankAccount; }
    public void setBankAccount(BankAccountEntityJpa bankAccount) { this.bankAccount = bankAccount; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getLimit() { return limit; }
    public void setLimit(BigDecimal limit) { this.limit = limit; }
    public Integer getClosingDay() { return closingDay; }
    public void setClosingDay(Integer closingDay) { this.closingDay = closingDay; }
    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
