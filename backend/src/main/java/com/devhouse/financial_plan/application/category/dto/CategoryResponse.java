package com.devhouse.financial_plan.application.category.dto;

import com.devhouse.financial_plan.domain.enums.CategoryKind;

import java.util.List;

public record CategoryResponse(Long id, Integer version, String name, CategoryKind kind, boolean active, boolean system,
                               List<SubCategoryResponse> subCategories) {}
