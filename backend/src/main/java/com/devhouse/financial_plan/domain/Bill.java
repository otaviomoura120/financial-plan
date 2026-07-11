package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;
import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class Bill {

    private Long id;
    private Integer version;
    private Space space;
    private BillRecurring billRecurring;
    private String name;
    private Category category;
    private SubCategory subCategory;
    private LocalDate referenceMonth;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BillInstanceStatus status;
    private LocalDate paidDate;
    private Long paymentTransactionId;
    private Long bankAccountId;
    private boolean deleted;
    private final Instant createdDate;
    private Instant updatedDate;

    public Bill(Long id, Integer version, Space space, BillRecurring billRecurring, String name, Category category,
                SubCategory subCategory, LocalDate referenceMonth, LocalDate dueDate, BigDecimal amount,
                BillInstanceStatus status, LocalDate paidDate, Long paymentTransactionId, Long bankAccountId,
                boolean deleted, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.space = space;
        this.billRecurring = billRecurring;
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.referenceMonth = referenceMonth;
        this.dueDate = dueDate;
        this.amount = amount;
        this.status = status;
        this.paidDate = paidDate;
        this.paymentTransactionId = paymentTransactionId;
        this.bankAccountId = bankAccountId;
        this.deleted = deleted;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (space == null) {
            throw new DomainException("Bill must be associated with a space");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Bill name cannot be empty");
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

    public void updateDetails(String name, Category category, SubCategory subCategory, BigDecimal amount, LocalDate dueDate) {
        if (!isPending()) {
            throw new DomainException("Cannot update a bill that is already paid");
        }
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.amount = amount;
        this.dueDate = dueDate;
        this.updatedDate = Instant.now();
    }

    public void detachFromRecurring() {
        this.billRecurring = null;
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
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking bill", new Exception());
        }
        this.version = version;
    }

    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public BillRecurring getBillRecurring() { return billRecurring; }
    public void setBillRecurring(BillRecurring billRecurring) { this.billRecurring = billRecurring; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public SubCategory getSubCategory() { return subCategory; }
    public void setSubCategory(SubCategory subCategory) { this.subCategory = subCategory; }
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
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
