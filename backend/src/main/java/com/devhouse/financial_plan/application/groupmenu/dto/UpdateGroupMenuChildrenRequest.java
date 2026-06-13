package com.devhouse.financial_plan.application.groupmenu.dto;

public record UpdateGroupMenuChildrenRequest(
        String name,
        String endpoint,
        String icon
) {}
