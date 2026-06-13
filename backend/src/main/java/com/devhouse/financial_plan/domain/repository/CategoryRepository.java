package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.Category;

import java.util.List;

public interface CategoryRepository {
    Category save(Category category);
    Category update(Category category);
    Category findById(Long id);
    List<Category> findBySpaceId(Long spaceId);
    void delete(Long id);
}
