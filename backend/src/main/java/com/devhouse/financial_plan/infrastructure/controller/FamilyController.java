package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.family.*;
import com.devhouse.financial_plan.application.family.dto.CreateFamilyRequest;
import com.devhouse.financial_plan.application.family.dto.FamilyResponse;
import com.devhouse.financial_plan.application.family.dto.UpdateFamilyRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/families")
public class FamilyController {

    private final CreateFamilyService createFamilyService;
    private final UpdateFamilyService updateFamilyService;
    private final DeleteFamilyService deleteFamilyService;
    private final AddFamilyMemberService addFamilyMemberService;
    private final RemoveFamilyMemberService removeFamilyMemberService;

    public FamilyController(CreateFamilyService createFamilyService, UpdateFamilyService updateFamilyService, DeleteFamilyService deleteFamilyService, AddFamilyMemberService addFamilyMemberService, RemoveFamilyMemberService removeFamilyMemberService) {
        this.createFamilyService = createFamilyService;
        this.updateFamilyService = updateFamilyService;
        this.deleteFamilyService = deleteFamilyService;
        this.addFamilyMemberService = addFamilyMemberService;
        this.removeFamilyMemberService = removeFamilyMemberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyResponse create(@RequestBody CreateFamilyRequest request) {
        return createFamilyService.execute(request);
    }

    @PutMapping("/{id}")
    public FamilyResponse update(@PathVariable Long id, @RequestBody UpdateFamilyRequest request) {
        return updateFamilyService.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteFamilyService.execute(id);
    }

    @PostMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMember(@PathVariable Long id, @PathVariable Long userId) {
        addFamilyMemberService.execute(id, userId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        removeFamilyMemberService.execute(id, userId);
    }
}
