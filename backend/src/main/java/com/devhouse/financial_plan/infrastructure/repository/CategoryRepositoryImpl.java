package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryRepositoryImpl implements CategoryRepository {

    @Override
    public Category save(Category category) { return null; }

    @Override
    public Category update(Category category) { return null; }

    @Override
    public Category findById(Long id) { return null; }

    @Override
    public List<Category> findAllActive() { return List.of(); }

    @Override
    public void delete(Long id) {}
}
