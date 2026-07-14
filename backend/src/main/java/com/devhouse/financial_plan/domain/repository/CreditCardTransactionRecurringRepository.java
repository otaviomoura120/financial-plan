package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;

import java.util.List;

public interface CreditCardTransactionRecurringRepository {
    CreditCardTransactionRecurring save(CreditCardTransactionRecurring creditCardTransactionRecurring);
    CreditCardTransactionRecurring update(CreditCardTransactionRecurring creditCardTransactionRecurring);
    CreditCardTransactionRecurring findById(Long id);
    List<CreditCardTransactionRecurring> findBySpaceId(Long spaceId);
    List<CreditCardTransactionRecurring> findByCreditCardId(Long creditCardId);
    void delete(Long id);
}
