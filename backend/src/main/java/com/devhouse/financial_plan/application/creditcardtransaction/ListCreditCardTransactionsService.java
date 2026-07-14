package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ListCreditCardTransactionsService {

    private final CreditCardTransactionRepository creditCardTransactionRepository;

    public ListCreditCardTransactionsService(CreditCardTransactionRepository creditCardTransactionRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
    }

    public List<CreditCardTransactionResponse> execute(Long spaceId, Long creditCardId, Long categoryId,
                                                         Long subCategoryId, LocalDate from, LocalDate to, LocalDate referenceMonth,
                                                         LocalDate competenceMonth) {
        Map<String, BigDecimal> totalAmountByGroup = new HashMap<>();
        return creditCardTransactionRepository.findByFilter(spaceId, creditCardId, categoryId, subCategoryId, null, referenceMonth).stream()
                .filter(t -> matchesCompetenceMonth(t, from, to, competenceMonth))
                .map(t -> toResponse(t, resolveTotalAmount(t, totalAmountByGroup)))
                .toList();
    }

    private boolean matchesCompetenceMonth(CreditCardTransaction transaction, LocalDate from, LocalDate to, LocalDate competenceMonth) {
        LocalDate transactionCompetenceMonth = transaction.getCompetenceMonth();
        if (from != null && transactionCompetenceMonth.isBefore(from)) {
            return false;
        }
        if (to != null && transactionCompetenceMonth.isAfter(to)) {
            return false;
        }
        return competenceMonth == null || competenceMonth.equals(transactionCompetenceMonth);
    }

    private BigDecimal resolveTotalAmount(CreditCardTransaction transaction, Map<String, BigDecimal> cache) {
        if (transaction.getTotalInstallments() == null || transaction.getTotalInstallments() <= 1) {
            return transaction.getAmount();
        }
        return cache.computeIfAbsent(transaction.getInstallmentGroupId(), groupId ->
                creditCardTransactionRepository.findByInstallmentGroupId(groupId).stream()
                        .map(CreditCardTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private CreditCardTransactionResponse toResponse(CreditCardTransaction t, BigDecimal totalAmount) {
        return new CreditCardTransactionResponse(t.getId(), t.getVersion(), t.getCreditCard().getId(), t.getUser().getId(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getSubCategory() != null ? t.getSubCategory().getId() : null, t.getAmount(), t.getPurchaseDate(),
                t.getDescription(), t.getReferenceMonth(), t.getCompetenceMonth(), t.getInstallmentGroupId(), t.getInstallmentNumber(),
                t.getTotalInstallments(), t.isAnticipated(), t.getOriginalReferenceMonth(), t.getCreatedDate(), totalAmount);
    }
}
