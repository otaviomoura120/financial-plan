package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.report.GenerateReportService;
import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest;
import com.devhouse.financial_plan.application.report.dto.ReportResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final GenerateReportService generateReportService;

    public ReportController(GenerateReportService generateReportService) {
        this.generateReportService = generateReportService;
    }

    @PostMapping
    public ReportResponse generate(@RequestBody ReportFilterRequest filter) {
        return generateReportService.execute(filter);
    }
}
