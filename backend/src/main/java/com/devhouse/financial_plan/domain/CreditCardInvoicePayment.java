package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class CreditCardInvoicePayment {

    private Long id;
    private Integer version;
    private CreditCard creditCard;
    private LocalDate referenceMonth;
    private LocalDate dueDate;
    private BigDecimal paidAmount;
    private LocalDate paidDate;
    private Long paymentTransactionId;
    private Long bankAccountId;
    private final Instant createdDate;
    private Instant updatedDate;

    public CreditCardInvoicePayment(Long id, Integer version, CreditCard creditCard, LocalDate referenceMonth, LocalDate dueDate, BigDecimal paidAmount, LocalDate paidDate, Long paymentTransactionId, Long bankAccountId, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.creditCard = creditCard;
        this.referenceMonth = referenceMonth;
        this.dueDate = dueDate;
        this.paidAmount = paidAmount;
        this.paidDate = paidDate;
        this.paymentTransactionId = paymentTransactionId;
        this.bankAccountId = bankAccountId;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (creditCard == null) {
            throw new DomainException("Credit card invoice payment must be associated with a credit card");
        }
        if (referenceMonth == null) {
            throw new DomainException("Reference month is required");
        }
        if (dueDate == null) {
            throw new DomainException("Due date is required");
        }
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Paid amount must be positive");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking creditCardInvoicePayment", new Exception());
        }
        this.version = version;
    }

    public CreditCard getCreditCard() { return creditCard; }
    public void setCreditCard(CreditCard creditCard) { this.creditCard = creditCard; }
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
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
