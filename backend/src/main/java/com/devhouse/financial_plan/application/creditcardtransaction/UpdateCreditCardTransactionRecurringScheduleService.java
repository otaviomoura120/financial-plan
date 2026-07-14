package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRecurringScheduleRequest;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateCreditCardTransactionRecurringScheduleService {

    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;

    public UpdateCreditCardTransactionRecurringScheduleService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
    }

    public CreditCardTransactionRecurringResponse execute(Long id, UpdateCreditCardTransactionRecurringScheduleRequest request) {
        CreditCardTransactionRecurring recurring = creditCardTransactionRecurringRepository.findById(id);
        if (recurring == null) {
            throw new DomainException("Credit card transaction recurring not found");
        }
        recurring.setVersion(request.version());
        recurring.updateSchedule(request.startDate());
        recurring.validate();
        CreditCardTransactionRecurring updated = creditCardTransactionRecurringRepository.update(recurring);
        return toResponse(updated);
    }

    private CreditCardTransactionRecurringResponse toResponse(CreditCardTransactionRecurring recurring) {
        return new CreditCardTransactionRecurringResponse(recurring.getId(), recurring.getVersion(), recurring.getCreditCard().getId(),
                recurring.getUser().getId(), recurring.getCategory() != null ? recurring.getCategory().getId() : null,
                recurring.getSubCategory() != null ? recurring.getSubCategory().getId() : null, recurring.getDescription(),
                recurring.getDefaultAmount(), recurring.getStartDate(), recurring.isActive(), recurring.getCreatedDate());
    }
}
