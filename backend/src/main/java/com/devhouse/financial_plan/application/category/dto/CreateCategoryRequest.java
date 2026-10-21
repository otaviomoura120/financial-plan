package com.devhouse.financial_plan.application.category.dto;

import com.devhouse.financial_plan.domain.enums.CategoryKind;

public record CreateCategoryRequest(String name, CategoryKind kind) {}
