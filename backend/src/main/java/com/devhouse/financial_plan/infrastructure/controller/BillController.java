package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.infrastructure.security.SecurityService;
import com.devhouse.financial_plan.application.bill.CreateBillService;
import com.devhouse.financial_plan.application.bill.DeleteBillRecurringService;
import com.devhouse.financial_plan.application.bill.ListBillsService;
import com.devhouse.financial_plan.application.bill.UpdateBillRecurringService;
import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.CreateBillRequest;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bills")
public class BillController {

    private final CreateBillService createBillService;
    private final UpdateBillRecurringService updateBillRecurringService;
    private final DeleteBillRecurringService deleteBillRecurringService;
    private final ListBillsService listBillsService;

    public BillController(CreateBillService createBillService, UpdateBillRecurringService updateBillRecurringService,
                           DeleteBillRecurringService deleteBillRecurringService, ListBillsService listBillsService) {
        this.createBillService = createBillService;
        this.updateBillRecurringService = updateBillRecurringService;
        this.deleteBillRecurringService = deleteBillRecurringService;
        this.listBillsService = listBillsService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<BillResponse> list(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, Authentication authentication, HttpServletRequest request) {
        return listBillsService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillResponse create(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @RequestBody CreateBillRequest body, Authentication authentication, HttpServletRequest request) {
        return createBillService.execute(spaceId, body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillResponse update(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id, @RequestBody UpdateBillRecurringRequest body, Authentication authentication, HttpServletRequest request) {
        return updateBillRecurringService.execute(spaceId, id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteBillRecurringService.execute(spaceId, id);
    }
}
