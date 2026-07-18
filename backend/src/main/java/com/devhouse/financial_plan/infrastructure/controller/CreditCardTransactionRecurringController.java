package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.creditcardtransaction.CreateCreditCardTransactionRecurringService;
import com.devhouse.financial_plan.application.creditcardtransaction.DeleteCreditCardTransactionRecurringService;
import com.devhouse.financial_plan.application.creditcardtransaction.ListCreditCardTransactionRecurringsService;
import com.devhouse.financial_plan.application.creditcardtransaction.UpdateCreditCardTransactionRecurringService;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreateCreditCardTransactionRecurringRequest;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRecurringRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/credit-card-transactions/recurring")
public class CreditCardTransactionRecurringController {

    private final CreateCreditCardTransactionRecurringService createCreditCardTransactionRecurringService;
    private final UpdateCreditCardTransactionRecurringService updateCreditCardTransactionRecurringService;
    private final DeleteCreditCardTransactionRecurringService deleteCreditCardTransactionRecurringService;
    private final ListCreditCardTransactionRecurringsService listCreditCardTransactionRecurringsService;

    public CreditCardTransactionRecurringController(CreateCreditCardTransactionRecurringService createCreditCardTransactionRecurringService,
                                                     UpdateCreditCardTransactionRecurringService updateCreditCardTransactionRecurringService,
                                                     DeleteCreditCardTransactionRecurringService deleteCreditCardTransactionRecurringService,
                                                     ListCreditCardTransactionRecurringsService listCreditCardTransactionRecurringsService) {
        this.createCreditCardTransactionRecurringService = createCreditCardTransactionRecurringService;
        this.updateCreditCardTransactionRecurringService = updateCreditCardTransactionRecurringService;
        this.deleteCreditCardTransactionRecurringService = deleteCreditCardTransactionRecurringService;
        this.listCreditCardTransactionRecurringsService = listCreditCardTransactionRecurringsService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<CreditCardTransactionRecurringResponse> list(@RequestParam Long spaceId, Authentication authentication, HttpServletRequest request) {
        return listCreditCardTransactionRecurringsService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CreditCardTransactionRecurringResponse create(@RequestBody CreateCreditCardTransactionRecurringRequest body,
                                                          Authentication authentication, HttpServletRequest request) {
        return createCreditCardTransactionRecurringService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CreditCardTransactionRecurringResponse update(@PathVariable Long id, @RequestBody UpdateCreditCardTransactionRecurringRequest body,
                                                          Authentication authentication, HttpServletRequest request) {
        return updateCreditCardTransactionRecurringService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteCreditCardTransactionRecurringService.execute(id);
    }
}
