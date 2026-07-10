package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.CreditCardTransaction;

import java.time.LocalDate;
import java.util.List;

public interface CreditCardTransactionRepository {
    CreditCardTransaction save(CreditCardTransaction creditCardTransaction);
    CreditCardTransaction update(CreditCardTransaction creditCardTransaction);
    CreditCardTransaction findById(Long id);
    List<CreditCardTransaction> findByFilter(Long spaceId, Long creditCardId, Long categoryId, Long subCategoryId, Long userId, LocalDate from, LocalDate to, LocalDate referenceMonth);
    List<CreditCardTransaction> findByInstallmentGroupId(String installmentGroupId);
    List<CreditCardTransaction> findByCreditCardId(Long creditCardId);
    List<CreditCardTransaction> findByCreditCardIdAndReferenceMonth(Long creditCardId, LocalDate referenceMonth);
    void delete(Long id);
}
