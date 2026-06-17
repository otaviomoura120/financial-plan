package com.devhouse.financial_plan.application.groupmenu.dto;

public record UpdateGroupMenuChildrenRequest(
        Integer version,
        String name,
        String endpoint,
        String icon
) {}
