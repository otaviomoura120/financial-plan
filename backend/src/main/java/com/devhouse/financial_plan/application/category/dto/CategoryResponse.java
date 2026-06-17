package com.devhouse.financial_plan.application.category.dto;

import java.util.List;

public record CategoryResponse(Long id, Integer version, String name, boolean active, List<SubCategoryResponse> subCategories) {}
