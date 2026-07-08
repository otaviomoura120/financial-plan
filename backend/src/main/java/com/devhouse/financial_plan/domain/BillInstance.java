package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;
import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class BillInstance {

    private Long id;
    private Integer version;
    private Bill bill;
    private LocalDate referenceMonth;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BillInstanceStatus status;
    private LocalDate paidDate;
    private Long paymentTransactionId;
    private Long bankAccountId;
    private final Instant createdDate;
    private Instant updatedDate;

    public BillInstance(Long id, Integer version, Bill bill, LocalDate referenceMonth, LocalDate dueDate, BigDecimal amount,
                         BillInstanceStatus status, LocalDate paidDate, Long paymentTransactionId, Long bankAccountId,
                         Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.bill = bill;
        this.referenceMonth = referenceMonth;
        this.dueDate = dueDate;
        this.amount = amount;
        this.status = status;
        this.paidDate = paidDate;
        this.paymentTransactionId = paymentTransactionId;
        this.bankAccountId = bankAccountId;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (bill == null) {
            throw new DomainException("Bill instance must be associated with a bill");
        }
        if (referenceMonth == null) {
            throw new DomainException("Reference month is required");
        }
        if (dueDate == null) {
            throw new DomainException("Due date is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Amount must be positive");
        }
    }

    public void updateAmount(BigDecimal newAmount) {
        if (!isPending()) {
            throw new DomainException("Cannot update the amount of a bill instance that is already paid");
        }
        this.amount = newAmount;
        this.updatedDate = Instant.now();
    }

    public void markAsPaid(LocalDate paidDate, Long paymentTransactionId, Long bankAccountId) {
        if (!isPending()) {
            throw new DomainException("Bill instance is already paid");
        }
        this.status = BillInstanceStatus.PAID;
        this.paidDate = paidDate;
        this.paymentTransactionId = paymentTransactionId;
        this.bankAccountId = bankAccountId;
        this.updatedDate = Instant.now();
    }

    public void revertToPending() {
        this.status = BillInstanceStatus.PENDING;
        this.paidDate = null;
        this.paymentTransactionId = null;
        this.bankAccountId = null;
        this.updatedDate = Instant.now();
    }

    public boolean isPending() {
        return BillInstanceStatus.PENDING.equals(status);
    }

    public boolean isPaid() {
        return BillInstanceStatus.PAID.equals(status);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking billInstance", new Exception());
        }
        this.version = version;
    }

    public Bill getBill() { return bill; }
    public void setBill(Bill bill) { this.bill = bill; }
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
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
