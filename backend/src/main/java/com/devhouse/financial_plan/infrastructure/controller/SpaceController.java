package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.space.AddSpaceMemberService;
import com.devhouse.financial_plan.application.space.CreateSpaceService;
import com.devhouse.financial_plan.application.space.DeleteSpaceService;
import com.devhouse.financial_plan.application.space.ListUserSpacesService;
import com.devhouse.financial_plan.application.space.RemoveSpaceMemberService;
import com.devhouse.financial_plan.application.space.UpdateSpaceService;
import com.devhouse.financial_plan.application.space.dto.AddSpaceMemberRequest;
import com.devhouse.financial_plan.application.space.dto.CreateSpaceRequest;
import com.devhouse.financial_plan.application.space.dto.SpaceResponse;
import com.devhouse.financial_plan.application.space.dto.UpdateSpaceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces")
public class SpaceController {

    private final CreateSpaceService createSpaceService;
    private final UpdateSpaceService updateSpaceService;
    private final DeleteSpaceService deleteSpaceService;
    private final AddSpaceMemberService addSpaceMemberService;
    private final RemoveSpaceMemberService removeSpaceMemberService;
    private final ListUserSpacesService listUserSpacesService;

    public SpaceController(CreateSpaceService createSpaceService, UpdateSpaceService updateSpaceService,
                           DeleteSpaceService deleteSpaceService, AddSpaceMemberService addSpaceMemberService,
                           RemoveSpaceMemberService removeSpaceMemberService, ListUserSpacesService listUserSpacesService) {
        this.createSpaceService = createSpaceService;
        this.updateSpaceService = updateSpaceService;
        this.deleteSpaceService = deleteSpaceService;
        this.addSpaceMemberService = addSpaceMemberService;
        this.removeSpaceMemberService = removeSpaceMemberService;
        this.listUserSpacesService = listUserSpacesService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponse create(@RequestBody CreateSpaceRequest request) {
        return createSpaceService.execute(request);
    }

    @PutMapping("/{id}")
    public SpaceResponse update(@PathVariable Long id, @RequestBody UpdateSpaceRequest request) {
        return updateSpaceService.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteSpaceService.execute(id);
    }

    @GetMapping("/user/{userId}")
    public List<SpaceResponse> listByUser(@PathVariable Long userId) {
        return listUserSpacesService.execute(userId);
    }

    @PostMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMember(@PathVariable Long id, @PathVariable Long userId, @RequestBody AddSpaceMemberRequest request) {
        addSpaceMemberService.execute(id, userId, request);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        removeSpaceMemberService.execute(id, userId);
    }
}
