package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.billinstance.CreateBillInstanceService;
import com.devhouse.financial_plan.application.billinstance.DeleteBillService;
import com.devhouse.financial_plan.application.billinstance.ListBillInstancesService;
import com.devhouse.financial_plan.application.billinstance.PayBillInstanceService;
import com.devhouse.financial_plan.application.billinstance.UndoBillInstancePaymentService;
import com.devhouse.financial_plan.application.billinstance.UpdateBillService;
import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.application.billinstance.dto.CreateBillInstanceRequest;
import com.devhouse.financial_plan.application.billinstance.dto.PayBillInstanceRequest;
import com.devhouse.financial_plan.application.billinstance.dto.UpdateBillInstanceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final CreateBillInstanceService createBillInstanceService;
    private final UpdateBillService updateBillService;
    private final DeleteBillService deleteBillService;
    private final PayBillInstanceService payBillInstanceService;
    private final UndoBillInstancePaymentService undoBillInstancePaymentService;

    public BillInstanceController(ListBillInstancesService listBillInstancesService,
                                   CreateBillInstanceService createBillInstanceService, UpdateBillService updateBillService,
                                   DeleteBillService deleteBillService, PayBillInstanceService payBillInstanceService,
                                   UndoBillInstancePaymentService undoBillInstancePaymentService) {
        this.listBillInstancesService = listBillInstancesService;
        this.createBillInstanceService = createBillInstanceService;
        this.updateBillService = updateBillService;
        this.deleteBillService = deleteBillService;
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

    @PostMapping("/instances")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillInstanceResponse create(@RequestBody CreateBillInstanceRequest body, Authentication authentication, HttpServletRequest request) {
        return createBillInstanceService.execute(body);
    }

    @PutMapping("/instances/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillInstanceResponse update(@PathVariable Long id, @RequestBody UpdateBillInstanceRequest body,
                                        Authentication authentication, HttpServletRequest request) {
        return updateBillService.execute(id, body);
    }

    @DeleteMapping("/instances/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteBillService.execute(id);
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
