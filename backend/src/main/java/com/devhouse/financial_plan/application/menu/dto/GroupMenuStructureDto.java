package com.devhouse.financial_plan.application.menu.dto;

import java.util.List;

public record GroupMenuStructureDto(
        String name,
        String icon,
        List<GroupMenuChildrenDto> children
) {}
