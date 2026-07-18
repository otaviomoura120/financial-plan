package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.creditcardtransaction.AnticipateCreditCardInstallmentsService;
import com.devhouse.financial_plan.application.creditcardtransaction.CreateCreditCardTransactionService;
import com.devhouse.financial_plan.application.creditcardtransaction.DeleteCreditCardTransactionService;
import com.devhouse.financial_plan.application.creditcardtransaction.ListCreditCardTransactionsService;
import com.devhouse.financial_plan.application.creditcardtransaction.ListInstallmentGroupService;
import com.devhouse.financial_plan.application.creditcardtransaction.UpdateCreditCardTransactionService;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.AnticipateCreditCardInstallmentsRequest;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreateCreditCardTransactionRequest;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/credit-card-transactions")
public class CreditCardTransactionController {

    private final CreateCreditCardTransactionService createCreditCardTransactionService;
    private final UpdateCreditCardTransactionService updateCreditCardTransactionService;
    private final DeleteCreditCardTransactionService deleteCreditCardTransactionService;
    private final ListCreditCardTransactionsService listCreditCardTransactionsService;
    private final ListInstallmentGroupService listInstallmentGroupService;
    private final AnticipateCreditCardInstallmentsService anticipateCreditCardInstallmentsService;

    public CreditCardTransactionController(CreateCreditCardTransactionService createCreditCardTransactionService,
                                            UpdateCreditCardTransactionService updateCreditCardTransactionService,
                                            DeleteCreditCardTransactionService deleteCreditCardTransactionService,
                                            ListCreditCardTransactionsService listCreditCardTransactionsService,
                                            ListInstallmentGroupService listInstallmentGroupService,
                                            AnticipateCreditCardInstallmentsService anticipateCreditCardInstallmentsService) {
        this.createCreditCardTransactionService = createCreditCardTransactionService;
        this.updateCreditCardTransactionService = updateCreditCardTransactionService;
        this.deleteCreditCardTransactionService = deleteCreditCardTransactionService;
        this.listCreditCardTransactionsService = listCreditCardTransactionsService;
        this.listInstallmentGroupService = listInstallmentGroupService;
        this.anticipateCreditCardInstallmentsService = anticipateCreditCardInstallmentsService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<CreditCardTransactionResponse> list(@RequestParam Long spaceId,
                                                      @RequestParam(required = false) Long creditCardId,
                                                      @RequestParam(required = false) Long categoryId,
                                                      @RequestParam(required = false) Long subCategoryId,
                                                      @RequestParam(required = false) LocalDate from,
                                                      @RequestParam(required = false) LocalDate to,
                                                      @RequestParam(required = false) LocalDate referenceMonth,
                                                      @RequestParam(required = false) LocalDate competenceMonth,
                                                      Authentication authentication, HttpServletRequest request) {
        return listCreditCardTransactionsService.execute(spaceId, creditCardId, categoryId, subCategoryId, from, to, referenceMonth, competenceMonth);
    }

    @GetMapping("/installment-groups/{installmentGroupId}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<CreditCardTransactionResponse> listInstallmentGroup(@PathVariable String installmentGroupId,
                                                                      Authentication authentication, HttpServletRequest request) {
        return listInstallmentGroupService.execute(installmentGroupId);
    }

    @PostMapping("/installment-groups/{installmentGroupId}/anticipate")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<CreditCardTransactionResponse> anticipate(@PathVariable String installmentGroupId,
                                                            @RequestBody AnticipateCreditCardInstallmentsRequest body,
                                                            Authentication authentication, HttpServletRequest request) {
        return anticipateCreditCardInstallmentsService.execute(installmentGroupId, body.targetReferenceMonth(), body.installmentsToAnticipate());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CreditCardTransactionResponse create(@RequestBody CreateCreditCardTransactionRequest body, Authentication authentication, HttpServletRequest request) {
        return createCreditCardTransactionService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CreditCardTransactionResponse update(@PathVariable Long id, @RequestBody UpdateCreditCardTransactionRequest body, Authentication authentication, HttpServletRequest request) {
        return updateCreditCardTransactionService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, @RequestParam(required = false, defaultValue = "false") boolean includeFuture,
                        Authentication authentication, HttpServletRequest request) {
        deleteCreditCardTransactionService.execute(id, includeFuture);
    }
}
