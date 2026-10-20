package com.devhouse.financial_plan.application.category.dto;

import com.devhouse.financial_plan.domain.enums.CategoryKind;

public record UpdateCategoryRequest(Integer version, String name, CategoryKind kind) {}
