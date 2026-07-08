package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "credit_card_invoice_payments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"credit_card_id", "reference_month"}))
public class CreditCardInvoicePaymentEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCardEntityJpa creditCard;
    @Column(name = "reference_month")
    private LocalDate referenceMonth;
    @Column(name = "due_date")
    private LocalDate dueDate;
    @Column(name = "paid_amount")
    private BigDecimal paidAmount;
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
    public CreditCardEntityJpa getCreditCard() { return creditCard; }
    public void setCreditCard(CreditCardEntityJpa creditCard) { this.creditCard = creditCard; }
    public LocalDate getReferenceMonth() { return referenceMonth; }
    public void setReferenceMonth(LocalDate referenceMonth) { this.referenceMonth = referenceMonth; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    public Long getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(Long paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }
    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
