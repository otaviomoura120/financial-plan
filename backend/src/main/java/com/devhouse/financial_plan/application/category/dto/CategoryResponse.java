package com.devhouse.financial_plan.application.category.dto;

import java.util.List;

public record CategoryResponse(Long id, String name, boolean active, List<SubCategoryResponse> subCategories) {}
