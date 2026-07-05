package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.paymentmethod.CreatePaymentMethodService;
import com.devhouse.financial_plan.application.paymentmethod.DeletePaymentMethodService;
import com.devhouse.financial_plan.application.paymentmethod.ListPaymentMethodsService;
import com.devhouse.financial_plan.application.paymentmethod.UpdatePaymentMethodService;
import com.devhouse.financial_plan.application.paymentmethod.dto.CreatePaymentMethodRequest;
import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse;
import com.devhouse.financial_plan.application.paymentmethod.dto.UpdatePaymentMethodRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment-methods")
public class PaymentMethodController {

    private final CreatePaymentMethodService createPaymentMethodService;
    private final UpdatePaymentMethodService updatePaymentMethodService;
    private final DeletePaymentMethodService deletePaymentMethodService;
    private final ListPaymentMethodsService listPaymentMethodsService;

    public PaymentMethodController(CreatePaymentMethodService createPaymentMethodService, UpdatePaymentMethodService updatePaymentMethodService, DeletePaymentMethodService deletePaymentMethodService, ListPaymentMethodsService listPaymentMethodsService) {
        this.createPaymentMethodService = createPaymentMethodService;
        this.updatePaymentMethodService = updatePaymentMethodService;
        this.deletePaymentMethodService = deletePaymentMethodService;
        this.listPaymentMethodsService = listPaymentMethodsService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<PaymentMethodResponse> list(@RequestParam Long spaceId, Authentication authentication, HttpServletRequest request) {
        return listPaymentMethodsService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public PaymentMethodResponse create(@RequestBody CreatePaymentMethodRequest body, Authentication authentication, HttpServletRequest request) {
        return createPaymentMethodService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public PaymentMethodResponse update(@PathVariable Long id, @RequestBody UpdatePaymentMethodRequest body, Authentication authentication, HttpServletRequest request) {
        return updatePaymentMethodService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deletePaymentMethodService.execute(id);
    }
}
