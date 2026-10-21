package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.infrastructure.security.SecurityService;
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
    private final UpdateCategoryStatusService updateCategoryStatusService;
    private final DeleteCategoryService deleteCategoryService;
    private final CreateSubCategoryService createSubCategoryService;
    private final UpdateSubCategoryService updateSubCategoryService;
    private final UpdateSubCategoryStatusService updateSubCategoryStatusService;
    private final DeleteSubCategoryService deleteSubCategoryService;
    private final ListCategoriesService listCategoriesService;

    public CategoryController(CreateCategoryService createCategoryService, UpdateCategoryService updateCategoryService,
                               UpdateCategoryStatusService updateCategoryStatusService, DeleteCategoryService deleteCategoryService,
                               CreateSubCategoryService createSubCategoryService, UpdateSubCategoryService updateSubCategoryService,
                               UpdateSubCategoryStatusService updateSubCategoryStatusService, DeleteSubCategoryService deleteSubCategoryService,
                               ListCategoriesService listCategoriesService) {
        this.createCategoryService = createCategoryService;
        this.updateCategoryService = updateCategoryService;
        this.updateCategoryStatusService = updateCategoryStatusService;
        this.deleteCategoryService = deleteCategoryService;
        this.createSubCategoryService = createSubCategoryService;
        this.updateSubCategoryService = updateSubCategoryService;
        this.updateSubCategoryStatusService = updateSubCategoryStatusService;
        this.deleteSubCategoryService = deleteSubCategoryService;
        this.listCategoriesService = listCategoriesService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<CategoryResponse> list(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, Authentication authentication, HttpServletRequest request) {
        return listCategoriesService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CategoryResponse create(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @RequestBody CreateCategoryRequest body,
                                   Authentication authentication, HttpServletRequest request) {
        return createCategoryService.execute(spaceId, body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CategoryResponse update(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id, @RequestBody UpdateCategoryRequest body,
            Authentication authentication, HttpServletRequest request) {
        return updateCategoryService.execute(spaceId, id, body);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public CategoryResponse updateStatus(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id, @RequestBody UpdateStatusRequest body,
            Authentication authentication, HttpServletRequest request) {
        return updateCategoryStatusService.execute(spaceId, id, Boolean.TRUE.equals(body.active()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteCategoryService.execute(spaceId, id);
    }

    @PostMapping("/subcategories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public SubCategoryResponse createSubCategory(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @RequestBody CreateSubCategoryRequest body,
                                                 Authentication authentication, HttpServletRequest request) {
        return createSubCategoryService.execute(spaceId, body);
    }

    @PutMapping("/subcategories/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public SubCategoryResponse updateSubCategory(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id, @RequestBody UpdateSubCategoryRequest body,
            Authentication authentication, HttpServletRequest request) {
        return updateSubCategoryService.execute(spaceId, id, body);
    }

    @PatchMapping("/subcategories/{id}/status")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public SubCategoryResponse updateSubCategoryStatus(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id, @RequestBody UpdateStatusRequest body,
            Authentication authentication, HttpServletRequest request) {
        return updateSubCategoryStatusService.execute(spaceId, id, Boolean.TRUE.equals(body.active()));
    }

    @DeleteMapping("/subcategories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void deleteSubCategory(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteSubCategoryService.execute(spaceId, id);
    }
}
