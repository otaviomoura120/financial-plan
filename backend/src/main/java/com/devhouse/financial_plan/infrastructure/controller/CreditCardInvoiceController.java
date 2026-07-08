package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.creditcardinvoice.ListCreditCardInvoicesService;
import com.devhouse.financial_plan.application.creditcardinvoice.PayCreditCardInvoiceService;
import com.devhouse.financial_plan.application.creditcardinvoice.UndoCreditCardInvoicePaymentService;
import com.devhouse.financial_plan.application.creditcardinvoice.dto.CreditCardInvoicePaymentResponse;
import com.devhouse.financial_plan.application.creditcardinvoice.dto.CreditCardInvoiceResponse;
import com.devhouse.financial_plan.application.creditcardinvoice.dto.PayCreditCardInvoiceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/credit-cards")
public class CreditCardInvoiceController {

    private final ListCreditCardInvoicesService listCreditCardInvoicesService;
    private final PayCreditCardInvoiceService payCreditCardInvoiceService;
    private final UndoCreditCardInvoicePaymentService undoCreditCardInvoicePaymentService;

    public CreditCardInvoiceController(ListCreditCardInvoicesService listCreditCardInvoicesService,
                                        PayCreditCardInvoiceService payCreditCardInvoiceService,
                                        UndoCreditCardInvoicePaymentService undoCreditCardInvoicePaymentService) {
        this.listCreditCardInvoicesService = listCreditCardInvoicesService;
        this.payCreditCardInvoiceService = payCreditCardInvoiceService;
        this.undoCreditCardInvoicePaymentService = undoCreditCardInvoicePaymentService;
    }

    @GetMapping("/invoices")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<CreditCardInvoiceResponse> list(@RequestParam Long spaceId,
                                                 @RequestParam(required = false) Long creditCardId,
                                                 @RequestParam(required = false) LocalDate from,
                                                 @RequestParam(required = false) LocalDate to,
                                                 Authentication authentication, HttpServletRequest request) {
        return listCreditCardInvoicesService.execute(spaceId, creditCardId, from, to);
    }

    @PostMapping("/{id}/invoices/{referenceMonth}/pay")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CreditCardInvoicePaymentResponse pay(@PathVariable Long id, @PathVariable LocalDate referenceMonth,
                                                 @RequestBody PayCreditCardInvoiceRequest body,
                                                 Authentication authentication, HttpServletRequest request) {
        return payCreditCardInvoiceService.execute(id, referenceMonth, body, authentication.getName());
    }

    @PostMapping("/{id}/invoices/{referenceMonth}/undo-payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void undoPayment(@PathVariable Long id, @PathVariable LocalDate referenceMonth,
                             Authentication authentication, HttpServletRequest request) {
        undoCreditCardInvoicePaymentService.execute(id, referenceMonth);
    }
}
