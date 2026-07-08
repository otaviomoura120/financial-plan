package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListCreditCardTransactionsService {

    private final CreditCardTransactionRepository creditCardTransactionRepository;

    public ListCreditCardTransactionsService(CreditCardTransactionRepository creditCardTransactionRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
    }

    public List<CreditCardTransactionResponse> execute(Long spaceId, Long creditCardId, Long categoryId,
                                                         Long subCategoryId, LocalDate from, LocalDate to) {
        return creditCardTransactionRepository.findByFilter(spaceId, creditCardId, categoryId, subCategoryId, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private CreditCardTransactionResponse toResponse(CreditCardTransaction t) {
        return new CreditCardTransactionResponse(t.getId(), t.getVersion(), t.getCreditCard().getId(), t.getUser().getId(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getSubCategory() != null ? t.getSubCategory().getId() : null, t.getAmount(), t.getPurchaseDate(),
                t.getDescription(), t.getReferenceMonth(), t.getInstallmentGroupId(), t.getInstallmentNumber(),
                t.getTotalInstallments(), t.isAnticipated(), t.getOriginalReferenceMonth(), t.getCreatedDate());
    }
}
