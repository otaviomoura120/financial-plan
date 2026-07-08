package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.billinstance.ListBillInstancesService;
import com.devhouse.financial_plan.application.billinstance.PayBillInstanceService;
import com.devhouse.financial_plan.application.billinstance.UndoBillInstancePaymentService;
import com.devhouse.financial_plan.application.billinstance.UpdateBillInstanceAmountService;
import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.application.billinstance.dto.PayBillInstanceRequest;
import com.devhouse.financial_plan.application.billinstance.dto.UpdateBillInstanceAmountRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bills")
public class BillInstanceController {

    private final ListBillInstancesService listBillInstancesService;
    private final UpdateBillInstanceAmountService updateBillInstanceAmountService;
    private final PayBillInstanceService payBillInstanceService;
    private final UndoBillInstancePaymentService undoBillInstancePaymentService;

    public BillInstanceController(ListBillInstancesService listBillInstancesService,
                                   UpdateBillInstanceAmountService updateBillInstanceAmountService,
                                   PayBillInstanceService payBillInstanceService,
                                   UndoBillInstancePaymentService undoBillInstancePaymentService) {
        this.listBillInstancesService = listBillInstancesService;
        this.updateBillInstanceAmountService = updateBillInstanceAmountService;
        this.payBillInstanceService = payBillInstanceService;
        this.undoBillInstancePaymentService = undoBillInstancePaymentService;
    }

    @GetMapping("/instances")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<BillInstanceResponse> list(@RequestParam Long spaceId,
                                            @RequestParam(required = false) LocalDate from,
                                            @RequestParam(required = false) LocalDate to,
                                            Authentication authentication, HttpServletRequest request) {
        return listBillInstancesService.execute(spaceId, from, to);
    }

    @PutMapping("/instances/{id}/amount")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillInstanceResponse updateAmount(@PathVariable Long id, @RequestBody UpdateBillInstanceAmountRequest body,
                                              Authentication authentication, HttpServletRequest request) {
        return updateBillInstanceAmountService.execute(id, body);
    }

    @PostMapping("/instances/{id}/pay")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillInstanceResponse pay(@PathVariable Long id, @RequestBody PayBillInstanceRequest body,
                                     Authentication authentication, HttpServletRequest request) {
        return payBillInstanceService.execute(id, body, authentication.getName());
    }

    @PostMapping("/instances/{id}/undo-payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void undoPayment(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        undoBillInstancePaymentService.execute(id);
    }
}
