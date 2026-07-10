package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BankAccountEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardInvoicePaymentEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCreditCardRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@Transactional
public class CreditCardInvoicePaymentRepositoryImpl implements CreditCardInvoicePaymentRepository {

    private final JpaCreditCardInvoicePaymentRepository jpaCreditCardInvoicePaymentRepository;
    private final JpaCreditCardRepository jpaCreditCardRepository;

    public CreditCardInvoicePaymentRepositoryImpl(JpaCreditCardInvoicePaymentRepository jpaCreditCardInvoicePaymentRepository,
                                                    JpaCreditCardRepository jpaCreditCardRepository) {
        this.jpaCreditCardInvoicePaymentRepository = jpaCreditCardInvoicePaymentRepository;
        this.jpaCreditCardRepository = jpaCreditCardRepository;
    }

    @Override
    public CreditCardInvoicePayment save(CreditCardInvoicePayment creditCardInvoicePayment) {
        CreditCardInvoicePaymentEntityJpa entity = new CreditCardInvoicePaymentEntityJpa();
        applyFields(creditCardInvoicePayment, entity);
        CreditCardInvoicePaymentEntityJpa saved = jpaCreditCardInvoicePaymentRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public CreditCardInvoicePayment findById(Long id) {
        return jpaCreditCardInvoicePaymentRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public CreditCardInvoicePayment findByCreditCardIdAndReferenceMonth(Long creditCardId, LocalDate referenceMonth) {
        return jpaCreditCardInvoicePaymentRepository.findByCreditCard_IdAndReferenceMonth(creditCardId, referenceMonth)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<CreditCardInvoicePayment> findByPaymentTransactionIdIn(List<Long> transactionIds) {
        return jpaCreditCardInvoicePaymentRepository.findByPaymentTransactionIdIn(transactionIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaCreditCardInvoicePaymentRepository.deleteById(id);
    }

    private void applyFields(CreditCardInvoicePayment creditCardInvoicePayment, CreditCardInvoicePaymentEntityJpa entity) {
        entity.setCreditCard(jpaCreditCardRepository.getReferenceById(creditCardInvoicePayment.getCreditCard().getId()));
        entity.setReferenceMonth(creditCardInvoicePayment.getReferenceMonth());
        entity.setDueDate(creditCardInvoicePayment.getDueDate());
        entity.setPaidAmount(creditCardInvoicePayment.getPaidAmount());
        entity.setPaidDate(creditCardInvoicePayment.getPaidDate());
        entity.setPaymentTransactionId(creditCardInvoicePayment.getPaymentTransactionId());
        entity.setBankAccountId(creditCardInvoicePayment.getBankAccountId());
        entity.setCreatedAt(creditCardInvoicePayment.getCreatedDate());
    }

    private CreditCardInvoicePayment toDomain(CreditCardInvoicePaymentEntityJpa entity) {
        CreditCard creditCard = entity.getCreditCard() != null ? buildCreditCard(entity.getCreditCard()) : null;
        return new CreditCardInvoicePayment(entity.getId(), entity.getVersion(), creditCard, entity.getReferenceMonth(),
                entity.getDueDate(), entity.getPaidAmount(), entity.getPaidDate(), entity.getPaymentTransactionId(),
                entity.getBankAccountId(), entity.getCreatedAt(), null);
    }

    private CreditCard buildCreditCard(CreditCardEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        BankAccount bankAccount = entity.getBankAccount() != null ? buildBankAccount(entity.getBankAccount()) : null;
        return new CreditCard(entity.getId(), entity.getVersion(), space, bankAccount, entity.getName(), entity.getLimit(),
                entity.getClosingDay(), entity.getDueDay(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private BankAccount buildBankAccount(BankAccountEntityJpa entity) {
        return new BankAccount(entity.getId(), entity.getVersion(), null, entity.getName(),
                entity.getBankName(), entity.getBalance(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
