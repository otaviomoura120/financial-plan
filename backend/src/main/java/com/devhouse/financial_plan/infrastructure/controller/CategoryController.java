package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.category.*;
import com.devhouse.financial_plan.application.category.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CreateCategoryService createCategoryService;
    private final UpdateCategoryService updateCategoryService;
    private final DeleteCategoryService deleteCategoryService;
    private final CreateSubCategoryService createSubCategoryService;
    private final UpdateSubCategoryService updateSubCategoryService;
    private final DeleteSubCategoryService deleteSubCategoryService;

    public CategoryController(CreateCategoryService createCategoryService, UpdateCategoryService updateCategoryService, DeleteCategoryService deleteCategoryService, CreateSubCategoryService createSubCategoryService, UpdateSubCategoryService updateSubCategoryService, DeleteSubCategoryService deleteSubCategoryService) {
        this.createCategoryService = createCategoryService;
        this.updateCategoryService = updateCategoryService;
        this.deleteCategoryService = deleteCategoryService;
        this.createSubCategoryService = createSubCategoryService;
        this.updateSubCategoryService = updateSubCategoryService;
        this.deleteSubCategoryService = deleteSubCategoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@RequestBody CreateCategoryRequest request) {
        return createCategoryService.execute(request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @RequestBody UpdateCategoryRequest request) {
        return updateCategoryService.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteCategoryService.execute(id);
    }

    @PostMapping("/subcategories")
    @ResponseStatus(HttpStatus.CREATED)
    public SubCategoryResponse createSubCategory(@RequestBody CreateSubCategoryRequest request) {
        return createSubCategoryService.execute(request);
    }

    @PutMapping("/subcategories/{id}")
    public SubCategoryResponse updateSubCategory(@PathVariable Long id, @RequestBody UpdateSubCategoryRequest request) {
        return updateSubCategoryService.execute(id, request);
    }

    @DeleteMapping("/subcategories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubCategory(@PathVariable Long id) {
        deleteSubCategoryService.execute(id);
    }
}
