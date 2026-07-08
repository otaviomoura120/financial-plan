package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.bill.CreateBillService;
import com.devhouse.financial_plan.application.bill.DeactivateBillService;
import com.devhouse.financial_plan.application.bill.ListBillsService;
import com.devhouse.financial_plan.application.bill.UpdateBillScheduleService;
import com.devhouse.financial_plan.application.bill.UpdateBillService;
import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.CreateBillRequest;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRequest;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillScheduleRequest;
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
    private final UpdateBillService updateBillService;
    private final UpdateBillScheduleService updateBillScheduleService;
    private final DeactivateBillService deactivateBillService;
    private final ListBillsService listBillsService;

    public BillController(CreateBillService createBillService, UpdateBillService updateBillService,
                           UpdateBillScheduleService updateBillScheduleService, DeactivateBillService deactivateBillService,
                           ListBillsService listBillsService) {
        this.createBillService = createBillService;
        this.updateBillService = updateBillService;
        this.updateBillScheduleService = updateBillScheduleService;
        this.deactivateBillService = deactivateBillService;
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
    public BillResponse update(@PathVariable Long id, @RequestBody UpdateBillRequest body, Authentication authentication, HttpServletRequest request) {
        return updateBillService.execute(id, body);
    }

    @PutMapping("/{id}/schedule")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BillResponse updateSchedule(@PathVariable Long id, @RequestBody UpdateBillScheduleRequest body, Authentication authentication, HttpServletRequest request) {
        return updateBillScheduleService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void deactivate(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deactivateBillService.execute(id);
    }
}
