package com.devhouse.financial_plan.application.groupmenu.dto;

public record UpdateGroupMenuRequest(
        Integer version,
        String name,
        String icon
) {}
