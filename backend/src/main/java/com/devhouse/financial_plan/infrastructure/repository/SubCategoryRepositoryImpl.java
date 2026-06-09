package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubCategoryRepositoryImpl implements SubCategoryRepository {

    @Override
    public SubCategory save(SubCategory subCategory) { return null; }

    @Override
    public SubCategory update(SubCategory subCategory) { return null; }

    @Override
    public SubCategory findById(Long id) { return null; }

    @Override
    public List<SubCategory> findByCategoryId(Long categoryId) { return List.of(); }

    @Override
    public void delete(Long id) {}
}
