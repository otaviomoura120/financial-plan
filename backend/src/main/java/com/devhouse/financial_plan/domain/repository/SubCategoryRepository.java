package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.SubCategory;

import java.util.List;

public interface SubCategoryRepository {
    SubCategory save(SubCategory subCategory);
    SubCategory update(SubCategory subCategory);
    SubCategory findById(Long id);
    List<SubCategory> findByCategoryId(Long categoryId);
    void delete(Long id);
    boolean existsByCategoryId(Long categoryId);
}
