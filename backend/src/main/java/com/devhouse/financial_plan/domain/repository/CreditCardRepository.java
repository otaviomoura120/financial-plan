package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.CreditCard;

import java.util.List;

public interface CreditCardRepository {
    CreditCard save(CreditCard creditCard);
    CreditCard update(CreditCard creditCard);
    CreditCard findById(Long id);
    List<CreditCard> findBySpaceId(Long spaceId);
    void delete(Long id);
}
