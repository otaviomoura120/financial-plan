package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.bill.CreateBillService;
import com.devhouse.financial_plan.application.bill.DeactivateBillRecurringService;
import com.devhouse.financial_plan.application.bill.ListBillsService;
import com.devhouse.financial_plan.application.bill.UpdateBillRecurringScheduleService;
import com.devhouse.financial_plan.application.bill.UpdateBillRecurringService;
import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.CreateBillRequest;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringRequest;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringScheduleRequest;
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
    private final UpdateBillRecurringScheduleService updateBillRecurringScheduleService;
    private final DeactivateBillRecurringService deactivateBillRecurringService;
    private final ListBillsService listBillsService;

    public BillController(CreateBillService createBillService, UpdateBillRecurringService updateBillRecurringService,
                           UpdateBillRecurringScheduleService updateBillRecurringScheduleService,
                           DeactivateBillRecurringService deactivateBillRecurringService, ListBillsService listBillsService) {
        this.createBillService = createBillService;
        this.updateBillRecurringService = updateBillRecurringService;
        this.updateBillRecurringScheduleService = updateBillRecurringScheduleService;
        this.deactivateBillRecurringService = deactivateBillRecurringService;
        this.listBillsService = listBillsService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<BillResponse> list(@RequestParam Long spaceId, Authentication authentication, HttpServletRequest request) {
        return listBillsService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillResponse create(@RequestBody CreateBillRequest body, Authentication authentication, HttpServletRequest request) {
        return createBillService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillResponse update(@PathVariable Long id, @RequestBody UpdateBillRecurringRequest body, Authentication authentication, HttpServletRequest request) {
        return updateBillRecurringService.execute(id, body);
    }

    @PutMapping("/{id}/schedule")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillResponse updateSchedule(@PathVariable Long id, @RequestBody UpdateBillRecurringScheduleRequest body, Authentication authentication, HttpServletRequest request) {
        return updateBillRecurringScheduleService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void deactivate(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deactivateBillRecurringService.execute(id);
    }
}
