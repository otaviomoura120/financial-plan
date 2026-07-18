package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class ListInstallmentGroupService {

    private final CreditCardTransactionRepository creditCardTransactionRepository;

    public ListInstallmentGroupService(CreditCardTransactionRepository creditCardTransactionRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
    }

    public List<CreditCardTransactionResponse> execute(String installmentGroupId) {
        List<CreditCardTransaction> group = creditCardTransactionRepository.findByInstallmentGroupId(installmentGroupId);
        BigDecimal totalAmount = group.stream().map(CreditCardTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return group.stream()
                .sorted(Comparator.comparing(CreditCardTransaction::getInstallmentNumber))
                .map(t -> toResponse(t, totalAmount))
                .toList();
    }

    private CreditCardTransactionResponse toResponse(CreditCardTransaction t, BigDecimal totalAmount) {
        return new CreditCardTransactionResponse(t.getId(), t.getVersion(), t.getCreditCard().getId(), t.getUser().getId(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getSubCategory() != null ? t.getSubCategory().getId() : null, t.getAmount(), t.getPurchaseDate(),
                t.getDescription(), t.getReferenceMonth(), t.getCompetenceMonth(), t.getInstallmentGroupId(), t.getInstallmentNumber(),
                t.getTotalInstallments(), t.isAnticipated(), t.getOriginalReferenceMonth(), t.getCreatedDate(), totalAmount);
    }
}
