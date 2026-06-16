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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #id)")
    public SpaceResponse update(@PathVariable Long id, @RequestBody UpdateSpaceRequest body,
                                Authentication authentication, HttpServletRequest request) {
        return updateSpaceService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #id)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteSpaceService.execute(id);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@securityService.isSelf(authentication, #userId)")
    public List<SpaceResponse> listByUser(@PathVariable Long userId, Authentication authentication) {
        return listUserSpacesService.execute(userId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #id)")
    public void removeMember(@PathVariable Long id, @PathVariable Long userId,
                             Authentication authentication, HttpServletRequest request) {
        removeSpaceMemberService.execute(id, userId);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #id)")
    public List<SpaceMemberResponse> listMembers(@PathVariable Long id,
                                                 Authentication authentication, HttpServletRequest request) {
        return listSpaceMembersService.execute(id);
    }

    @PutMapping("/{id}/members/{userId}")
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #id)")
    public SpaceMemberResponse updateMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                                @RequestBody UpdateSpaceMemberRequest body,
                                                Authentication authentication, HttpServletRequest request) {
        return updateSpaceMemberRoleService.execute(id, userId, body);
    }

    @PostMapping("/{id}/invites")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #id)")
    public void inviteMember(@PathVariable Long id, @RequestBody InviteRequest body,
                             Authentication authentication, HttpServletRequest request) {
        inviteSpaceMemberService.execute(id, body);
    }

    @GetMapping("/{id}/invites")
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #id)")
    public List<SpaceInviteResponse> listInvites(@PathVariable Long id,
                                                 Authentication authentication, HttpServletRequest request) {
        return listSpaceInvitesService.execute(id);
    }

    @DeleteMapping("/{id}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #id)")
    public void cancelInvite(@PathVariable Long id, @PathVariable Long inviteId,
                             Authentication authentication, HttpServletRequest request) {
        cancelSpaceInviteService.execute(id, inviteId);
    }
}
