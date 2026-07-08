package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.creditcard.CreateCreditCardService;
import com.devhouse.financial_plan.application.creditcard.DeactivateCreditCardService;
import com.devhouse.financial_plan.application.creditcard.ListCreditCardsService;
import com.devhouse.financial_plan.application.creditcard.UpdateCreditCardService;
import com.devhouse.financial_plan.application.creditcard.dto.CreateCreditCardRequest;
import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse;
import com.devhouse.financial_plan.application.creditcard.dto.UpdateCreditCardRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/credit-cards")
public class CreditCardController {

    private final CreateCreditCardService createCreditCardService;
    private final UpdateCreditCardService updateCreditCardService;
    private final DeactivateCreditCardService deactivateCreditCardService;
    private final ListCreditCardsService listCreditCardsService;

    public CreditCardController(CreateCreditCardService createCreditCardService, UpdateCreditCardService updateCreditCardService,
                                 DeactivateCreditCardService deactivateCreditCardService, ListCreditCardsService listCreditCardsService) {
        this.createCreditCardService = createCreditCardService;
        this.updateCreditCardService = updateCreditCardService;
        this.deactivateCreditCardService = deactivateCreditCardService;
        this.listCreditCardsService = listCreditCardsService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<CreditCardResponse> list(@RequestParam Long spaceId, Authentication authentication, HttpServletRequest request) {
        return listCreditCardsService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CreditCardResponse create(@RequestBody CreateCreditCardRequest body, Authentication authentication, HttpServletRequest request) {
        return createCreditCardService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CreditCardResponse update(@PathVariable Long id, @RequestBody UpdateCreditCardRequest body, Authentication authentication, HttpServletRequest request) {
        return updateCreditCardService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void deactivate(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deactivateCreditCardService.execute(id);
    }
}
