package com.devhouse.financial_plan.application.category.dto;

public record SubCategoryResponse(Long id, Integer version, Long categoryId, String name, boolean active) {}
