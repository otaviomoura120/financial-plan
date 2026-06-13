package com.devhouse.financial_plan.application.groupmenu.dto;

public record CreateGroupMenuChildrenRequest(
        Long groupMenuId,
        String name,
        String endpoint,
        String icon
) {}
