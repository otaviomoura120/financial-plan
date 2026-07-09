package com.devhouse.financial_plan.application.creditcardinvoice;

import com.devhouse.financial_plan.application.creditcardinvoice.dto.CreditCardInvoicePaymentResponse;
import com.devhouse.financial_plan.application.creditcardinvoice.dto.PayCreditCardInvoiceRequest;
import com.devhouse.financial_plan.application.transaction.CreateTransactionService;
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardInvoiceCycle;
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayCreditCardInvoiceService {

    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;
    private final UserRepository userRepository;
    private final CreateTransactionService createTransactionService;

    public PayCreditCardInvoiceService(CreditCardRepository creditCardRepository,
                                        CreditCardTransactionRepository creditCardTransactionRepository,
                                        CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository,
                                        UserRepository userRepository, CreateTransactionService createTransactionService) {
        this.creditCardRepository = creditCardRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
        this.userRepository = userRepository;
        this.createTransactionService = createTransactionService;
    }

    @Transactional
    public CreditCardInvoicePaymentResponse execute(Long creditCardId, LocalDate referenceMonth, PayCreditCardInvoiceRequest request, String auth0Sub) {
        CreditCard creditCard = resolveCreditCard(creditCardId);
        User user = resolveUser(auth0Sub);
        rejectIfAlreadyPaid(creditCardId, referenceMonth);

        List<CreditCardTransaction> transactions = creditCardTransactionRepository.findByCreditCardIdAndReferenceMonth(creditCardId, referenceMonth);
        BigDecimal totalAmount = transactions.stream()
                .map(CreditCardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Invoice has no transactions to pay");
        }

        LocalDate dueDate = CreditCardInvoiceCycle.resolveDueDate(referenceMonth, creditCard.getClosingDay(), creditCard.getDueDay());

        CreateTransactionRequest transactionRequest = new CreateTransactionRequest(TransactionType.EXPENSE, user.getId(),
                request.bankAccountId(), null, request.categoryId(), request.subCategoryId(), request.paymentMethodId(), totalAmount,
                request.paidDate(), "Pagamento de fatura - " + creditCard.getName());
        TransactionResponse paymentTransaction = createTransactionService.execute(transactionRequest,
                TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, creditCard.getId());

        CreditCardInvoicePayment payment = new CreditCardInvoicePayment(null, 0, creditCard, referenceMonth, dueDate,
                totalAmount, request.paidDate(), paymentTransaction.id(), request.bankAccountId(), Instant.now(), null);
        payment.validate();
        CreditCardInvoicePayment saved = creditCardInvoicePaymentRepository.save(payment);
        return toResponse(saved);
    }

    private CreditCard resolveCreditCard(Long creditCardId) {
        CreditCard creditCard = creditCardId != null ? creditCardRepository.findById(creditCardId) : null;
        if (creditCard == null) {
            throw new DomainException("Credit card not found");
        }
        return creditCard;
    }

    private User resolveUser(String auth0Sub) {
        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            throw new DomainException("User not found");
        }
        return user;
    }

    private void rejectIfAlreadyPaid(Long creditCardId, LocalDate referenceMonth) {
        boolean alreadyPaid = creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(creditCardId, referenceMonth) != null;
        if (alreadyPaid) {
            throw new DomainException("Invoice already paid");
        }
    }

    private CreditCardInvoicePaymentResponse toResponse(CreditCardInvoicePayment payment) {
        return new CreditCardInvoicePaymentResponse(payment.getId(), payment.getCreditCard().getId(), payment.getReferenceMonth(),
                payment.getDueDate(), payment.getPaidAmount(), payment.getPaidDate(), payment.getPaymentTransactionId(),
                payment.getBankAccountId());
    }
}
