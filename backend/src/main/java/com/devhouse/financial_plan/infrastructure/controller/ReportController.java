package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.report.GenerateCategoryReportService;
import com.devhouse.financial_plan.application.report.GenerateReportService;
import com.devhouse.financial_plan.application.report.dto.CategoryReportFilterRequest;
import com.devhouse.financial_plan.application.report.dto.CategoryReportResponse;
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
    private final GenerateCategoryReportService generateCategoryReportService;

    public ReportController(GenerateReportService generateReportService, GenerateCategoryReportService generateCategoryReportService) {
        this.generateReportService = generateReportService;
        this.generateCategoryReportService = generateCategoryReportService;
    }

    @PostMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public ReportResponse generate(@RequestBody ReportFilterRequest body, Authentication authentication, HttpServletRequest request) {
        return generateReportService.execute(body);
    }

    @PostMapping("/by-category")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CategoryReportResponse generateByCategory(@RequestBody CategoryReportFilterRequest body, Authentication authentication, HttpServletRequest request) {
        return generateCategoryReportService.execute(body);
    }
}
