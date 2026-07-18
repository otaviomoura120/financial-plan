package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class AnticipateCreditCardInstallmentsService {

    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public AnticipateCreditCardInstallmentsService(CreditCardTransactionRepository creditCardTransactionRepository,
                                                    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public List<CreditCardTransactionResponse> execute(String installmentGroupId, LocalDate targetReferenceMonth, Integer installmentsToAnticipate) {
        List<CreditCardTransaction> group = creditCardTransactionRepository.findByInstallmentGroupId(installmentGroupId);
        if (group.isEmpty()) {
            throw new DomainException("Installment group not found");
        }
        if (installmentsToAnticipate == null || installmentsToAnticipate <= 0) {
            throw new DomainException("Installments to anticipate must be greater than zero");
        }

        Long creditCardId = group.get(0).getCreditCard().getId();
        boolean targetInvoiceAlreadyPaid = creditCardInvoicePaymentRepository
                .findByCreditCardIdAndReferenceMonth(creditCardId, targetReferenceMonth) != null;
        if (targetInvoiceAlreadyPaid) {
            throw new DomainException("Cannot anticipate into a paid invoice");
        }

        List<CreditCardTransaction> eligible = group.stream()
                .filter(installment -> installment.getReferenceMonth().isAfter(targetReferenceMonth))
                .sorted(Comparator.comparing(CreditCardTransaction::getInstallmentNumber).reversed())
                .toList();
        if (installmentsToAnticipate > eligible.size()) {
            throw new DomainException("Not enough remaining installments to anticipate");
        }

        for (int i = 0; i < installmentsToAnticipate; i++) {
            CreditCardTransaction installment = eligible.get(i);
            installment.anticipateTo(targetReferenceMonth);
            creditCardTransactionRepository.update(installment);
        }

        List<CreditCardTransaction> updatedGroup = creditCardTransactionRepository.findByInstallmentGroupId(installmentGroupId);
        BigDecimal totalAmount = updatedGroup.stream().map(CreditCardTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return updatedGroup.stream()
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
