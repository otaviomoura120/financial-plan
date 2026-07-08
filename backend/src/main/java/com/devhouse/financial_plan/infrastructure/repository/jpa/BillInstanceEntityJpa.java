package com.devhouse.financial_plan.infrastructure.repository.jpa;

import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bill_instances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bill_id", "reference_month"}))
public class BillInstanceEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private BillEntityJpa bill;
    @Column(name = "reference_month")
    private LocalDate referenceMonth;
    @Column(name = "due_date")
    private LocalDate dueDate;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private BillInstanceStatus status;
    @Column(name = "paid_date")
    private LocalDate paidDate;
    @Column(name = "payment_transaction_id")
    private Long paymentTransactionId;
    @Column(name = "bank_account_id")
    private Long bankAccountId;
    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public BillEntityJpa getBill() { return bill; }
    public void setBill(BillEntityJpa bill) { this.bill = bill; }
    public LocalDate getReferenceMonth() { return referenceMonth; }
    public void setReferenceMonth(LocalDate referenceMonth) { this.referenceMonth = referenceMonth; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BillInstanceStatus getStatus() { return status; }
    public void setStatus(BillInstanceStatus status) { this.status = status; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    public Long getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(Long paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }
    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
