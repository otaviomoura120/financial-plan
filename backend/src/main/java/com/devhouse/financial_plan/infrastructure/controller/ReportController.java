package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.report.GenerateReportService;
import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest;
import com.devhouse.financial_plan.application.report.dto.ReportResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final GenerateReportService generateReportService;

    public ReportController(GenerateReportService generateReportService) {
        this.generateReportService = generateReportService;
    }

    @PostMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public ReportResponse generate(@RequestBody ReportFilterRequest body, Authentication authentication, HttpServletRequest request) {
        return generateReportService.execute(body);
    }
}
