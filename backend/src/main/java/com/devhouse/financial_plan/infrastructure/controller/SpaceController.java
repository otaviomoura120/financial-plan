package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.space.CreateSpaceService;
import com.devhouse.financial_plan.application.space.DeleteSpaceService;
import com.devhouse.financial_plan.application.space.CancelSpaceInviteService;
import com.devhouse.financial_plan.application.space.InviteSpaceMemberService;
import com.devhouse.financial_plan.application.space.ListSpaceInvitesService;
import com.devhouse.financial_plan.application.space.ListSpaceMembersService;
import com.devhouse.financial_plan.application.space.ListUserSpacesService;
import com.devhouse.financial_plan.application.space.RemoveSpaceMemberService;
import com.devhouse.financial_plan.application.space.UpdateSpaceMemberRoleService;
import com.devhouse.financial_plan.application.space.UpdateSpaceService;
import com.devhouse.financial_plan.application.space.dto.CreateSpaceRequest;
import com.devhouse.financial_plan.application.space.dto.InviteRequest;
import com.devhouse.financial_plan.application.space.dto.SpaceInviteResponse;
import com.devhouse.financial_plan.application.space.dto.SpaceMemberResponse;
import com.devhouse.financial_plan.application.space.dto.SpaceResponse;
import com.devhouse.financial_plan.application.space.dto.UpdateSpaceMemberRequest;
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
    private final RemoveSpaceMemberService removeSpaceMemberService;
    private final ListUserSpacesService listUserSpacesService;
    private final ListSpaceMembersService listSpaceMembersService;
    private final UpdateSpaceMemberRoleService updateSpaceMemberRoleService;
    private final InviteSpaceMemberService inviteSpaceMemberService;
    private final ListSpaceInvitesService listSpaceInvitesService;
    private final CancelSpaceInviteService cancelSpaceInviteService;

    public SpaceController(CreateSpaceService createSpaceService, UpdateSpaceService updateSpaceService,
                           DeleteSpaceService deleteSpaceService,
                           RemoveSpaceMemberService removeSpaceMemberService, ListUserSpacesService listUserSpacesService,
                           ListSpaceMembersService listSpaceMembersService,
                           UpdateSpaceMemberRoleService updateSpaceMemberRoleService,
                           InviteSpaceMemberService inviteSpaceMemberService,
                           ListSpaceInvitesService listSpaceInvitesService,
                           CancelSpaceInviteService cancelSpaceInviteService) {
        this.createSpaceService = createSpaceService;
        this.updateSpaceService = updateSpaceService;
        this.deleteSpaceService = deleteSpaceService;
        this.removeSpaceMemberService = removeSpaceMemberService;
        this.listUserSpacesService = listUserSpacesService;
        this.listSpaceMembersService = listSpaceMembersService;
        this.updateSpaceMemberRoleService = updateSpaceMemberRoleService;
        this.inviteSpaceMemberService = inviteSpaceMemberService;
        this.listSpaceInvitesService = listSpaceInvitesService;
        this.cancelSpaceInviteService = cancelSpaceInviteService;
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

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        removeSpaceMemberService.execute(id, userId);
    }

    @GetMapping("/{id}/members")
    public List<SpaceMemberResponse> listMembers(@PathVariable Long id) {
        return listSpaceMembersService.execute(id);
    }

    @PutMapping("/{id}/members/{userId}")
    public SpaceMemberResponse updateMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                                @RequestBody UpdateSpaceMemberRequest request) {
        return updateSpaceMemberRoleService.execute(id, userId, request);
    }

    @PostMapping("/{id}/invites")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inviteMember(@PathVariable Long id, @RequestBody InviteRequest request) {
        inviteSpaceMemberService.execute(id, request);
    }

    @GetMapping("/{id}/invites")
    public List<SpaceInviteResponse> listInvites(@PathVariable Long id) {
        return listSpaceInvitesService.execute(id);
    }

    @DeleteMapping("/{id}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvite(@PathVariable Long id, @PathVariable Long inviteId) {
        cancelSpaceInviteService.execute(id, inviteId);
    }
}
