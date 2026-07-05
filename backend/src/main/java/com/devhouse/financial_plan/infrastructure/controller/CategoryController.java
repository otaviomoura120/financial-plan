package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.category.*;
import com.devhouse.financial_plan.application.category.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CreateCategoryService createCategoryService;
    private final UpdateCategoryService updateCategoryService;
    private final DeleteCategoryService deleteCategoryService;
    private final CreateSubCategoryService createSubCategoryService;
    private final UpdateSubCategoryService updateSubCategoryService;
    private final DeleteSubCategoryService deleteSubCategoryService;
    private final ListCategoriesService listCategoriesService;

    public CategoryController(CreateCategoryService createCategoryService, UpdateCategoryService updateCategoryService, DeleteCategoryService deleteCategoryService, CreateSubCategoryService createSubCategoryService, UpdateSubCategoryService updateSubCategoryService, DeleteSubCategoryService deleteSubCategoryService, ListCategoriesService listCategoriesService) {
        this.createCategoryService = createCategoryService;
        this.updateCategoryService = updateCategoryService;
        this.deleteCategoryService = deleteCategoryService;
        this.createSubCategoryService = createSubCategoryService;
        this.updateSubCategoryService = updateSubCategoryService;
        this.deleteSubCategoryService = deleteSubCategoryService;
        this.listCategoriesService = listCategoriesService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<CategoryResponse> list(@RequestParam Long spaceId, Authentication authentication, HttpServletRequest request) {
        return listCategoriesService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CategoryResponse create(@RequestBody CreateCategoryRequest body, Authentication authentication, HttpServletRequest request) {
        return createCategoryService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CategoryResponse update(@PathVariable Long id, @RequestBody UpdateCategoryRequest body, Authentication authentication, HttpServletRequest request) {
        return updateCategoryService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteCategoryService.execute(id);
    }

    @PostMapping("/subcategories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public SubCategoryResponse createSubCategory(@RequestBody CreateSubCategoryRequest body, Authentication authentication, HttpServletRequest request) {
        return createSubCategoryService.execute(body);
    }

    @PutMapping("/subcategories/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public SubCategoryResponse updateSubCategory(@PathVariable Long id, @RequestBody UpdateSubCategoryRequest body, Authentication authentication, HttpServletRequest request) {
        return updateSubCategoryService.execute(id, body);
    }

    @DeleteMapping("/subcategories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void deleteSubCategory(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteSubCategoryService.execute(id);
    }
}
