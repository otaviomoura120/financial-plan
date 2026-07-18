package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListCreditCardTransactionRecurringsService {

    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;

    public ListCreditCardTransactionRecurringsService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
    }

    public List<CreditCardTransactionRecurringResponse> execute(Long spaceId) {
        return creditCardTransactionRecurringRepository.findBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CreditCardTransactionRecurringResponse toResponse(CreditCardTransactionRecurring recurring) {
        return new CreditCardTransactionRecurringResponse(recurring.getId(), recurring.getVersion(), recurring.getCreditCard().getId(),
                recurring.getUser().getId(), recurring.getCategory() != null ? recurring.getCategory().getId() : null,
                recurring.getSubCategory() != null ? recurring.getSubCategory().getId() : null, recurring.getDescription(),
                recurring.getDefaultAmount(), recurring.getStartDate(), recurring.isActive(), recurring.getCreatedDate());
    }
}
