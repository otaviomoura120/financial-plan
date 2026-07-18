package com.devhouse.financial_plan.application.creditcardinvoice;

import com.devhouse.financial_plan.application.creditcardinvoice.dto.CreditCardInvoiceResponse;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardInvoiceCycle;
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListCreditCardInvoicesService {

    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public ListCreditCardInvoicesService(CreditCardRepository creditCardRepository,
                                          CreditCardTransactionRepository creditCardTransactionRepository,
                                          CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardRepository = creditCardRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    public List<CreditCardInvoiceResponse> execute(Long spaceId, Long creditCardId, LocalDate from, LocalDate to) {
        List<CreditCard> creditCards = creditCardRepository.findBySpaceId(spaceId).stream()
                .filter(creditCard -> creditCardId == null || creditCard.getId().equals(creditCardId))
                .toList();

        return creditCards.stream()
                .flatMap(creditCard -> buildInvoices(creditCard, from, to).stream())
                .toList();
    }

    private List<CreditCardInvoiceResponse> buildInvoices(CreditCard creditCard, LocalDate from, LocalDate to) {
        Map<LocalDate, List<CreditCardTransaction>> byReferenceMonth = creditCardTransactionRepository
                .findByCreditCardId(creditCard.getId()).stream()
                .collect(Collectors.groupingBy(CreditCardTransaction::getReferenceMonth));

        return byReferenceMonth.entrySet().stream()
                .map(entry -> toResponse(creditCard, entry.getKey(), entry.getValue()))
                .filter(invoice -> (from == null || !invoice.dueDate().isBefore(from))
                        && (to == null || !invoice.dueDate().isAfter(to)))
                .toList();
    }

    private CreditCardInvoiceResponse toResponse(CreditCard creditCard, LocalDate referenceMonth, List<CreditCardTransaction> transactions) {
        BigDecimal totalAmount = transactions.stream()
                .map(CreditCardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate closingDate = CreditCardInvoiceCycle.resolveClosingDate(YearMonth.from(referenceMonth), creditCard.getClosingDay());
        LocalDate dueDate = CreditCardInvoiceCycle.resolveDueDate(referenceMonth, creditCard.getClosingDay(), creditCard.getDueDay());
        CreditCardInvoicePayment payment = creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(creditCard.getId(), referenceMonth);
        boolean paid = payment != null;
        return new CreditCardInvoiceResponse(creditCard.getId(), creditCard.getName(), referenceMonth, closingDate, dueDate,
                totalAmount, paid, paid ? payment.getPaidDate() : null, paid ? payment.getPaidAmount() : null,
                paid ? payment.getPaymentTransactionId() : null);
    }
}
