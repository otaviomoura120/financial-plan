package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.dashboardwidget.GetDashboardWidgetPermissionsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard-widgets")
public class DashboardWidgetController {

    private final GetDashboardWidgetPermissionsService getDashboardWidgetPermissionsService;

    public DashboardWidgetController(GetDashboardWidgetPermissionsService getDashboardWidgetPermissionsService) {
        this.getDashboardWidgetPermissionsService = getDashboardWidgetPermissionsService;
    }

    @GetMapping
    public List<String> getAllowedWidgets(@RequestParam Long spaceId, Authentication authentication) {
        return getDashboardWidgetPermissionsService.execute(authentication.getName(), spaceId);
    }
}
