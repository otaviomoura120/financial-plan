package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.paymentmethod.CreatePaymentMethodService;
import com.devhouse.financial_plan.application.paymentmethod.DeletePaymentMethodService;
import com.devhouse.financial_plan.application.paymentmethod.ListPaymentMethodsService;
import com.devhouse.financial_plan.application.paymentmethod.UpdatePaymentMethodService;
import com.devhouse.financial_plan.application.paymentmethod.dto.CreatePaymentMethodRequest;
import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse;
import com.devhouse.financial_plan.application.paymentmethod.dto.UpdatePaymentMethodRequest;
import org.springframework.http.HttpStatus;
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
    public List<PaymentMethodResponse> list(@RequestParam Long spaceId) {
        return listPaymentMethodsService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse create(@RequestBody CreatePaymentMethodRequest request) {
        return createPaymentMethodService.execute(request);
    }

    @PutMapping("/{id}")
    public PaymentMethodResponse update(@PathVariable Long id, @RequestBody UpdatePaymentMethodRequest request) {
        return updatePaymentMethodService.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deletePaymentMethodService.execute(id);
    }
}
