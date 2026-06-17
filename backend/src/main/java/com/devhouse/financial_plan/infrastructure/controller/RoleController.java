package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.role.AssignRoleToUserService;
import com.devhouse.financial_plan.application.role.CreateRoleService;
import com.devhouse.financial_plan.application.role.DeleteRoleService;
import com.devhouse.financial_plan.application.role.GetRolePermissionsService;
import com.devhouse.financial_plan.application.role.GetRolesBySpaceService;
import com.devhouse.financial_plan.application.role.UpdateRolePermissionAccessService;
import com.devhouse.financial_plan.application.role.UpdateRoleService;
import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest;
import com.devhouse.financial_plan.application.role.dto.RoleEndpointPermissionResponse;
import com.devhouse.financial_plan.application.role.dto.RoleResponse;
import com.devhouse.financial_plan.application.role.dto.UpdateRolePermissionAccessRequest;
import com.devhouse.financial_plan.application.role.dto.UpdateRoleRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final CreateRoleService createRoleService;
    private final UpdateRoleService updateRoleService;
    private final DeleteRoleService deleteRoleService;
    private final GetRolesBySpaceService getRolesBySpaceService;
    private final AssignRoleToUserService assignRoleToUserService;
    private final GetRolePermissionsService getRolePermissionsService;
    private final UpdateRolePermissionAccessService updateRolePermissionAccessService;

    public RoleController(CreateRoleService createRoleService, UpdateRoleService updateRoleService,
                          DeleteRoleService deleteRoleService, GetRolesBySpaceService getRolesBySpaceService,
                          AssignRoleToUserService assignRoleToUserService,
                          GetRolePermissionsService getRolePermissionsService,
                          UpdateRolePermissionAccessService updateRolePermissionAccessService) {
        this.createRoleService = createRoleService;
        this.updateRoleService = updateRoleService;
        this.deleteRoleService = deleteRoleService;
        this.getRolesBySpaceService = getRolesBySpaceService;
        this.assignRoleToUserService = assignRoleToUserService;
        this.getRolePermissionsService = getRolePermissionsService;
        this.updateRolePermissionAccessService = updateRolePermissionAccessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #body.spaceId())")
    public RoleResponse create(@RequestBody CreateRoleRequest body, Authentication authentication, HttpServletRequest request) {
        return createRoleService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForRole(authentication, #request, #id)")
    public RoleResponse update(@PathVariable Long id, @RequestBody UpdateRoleRequest body, Authentication authentication, HttpServletRequest request) {
        return updateRoleService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForRole(authentication, #request, #id)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteRoleService.execute(id);
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #spaceId)")
    public List<RoleResponse> getBySpace(@RequestParam Long spaceId, Authentication authentication, HttpServletRequest request) {
        return getRolesBySpaceService.execute(spaceId);
    }

    @PutMapping("/{id}/assign-user/{userId}")
    @PreAuthorize("@securityService.userHasPermissionInSpace(authentication, #request, #spaceId)")
    public void assignRole(@PathVariable Long id, @PathVariable Long userId, @RequestParam Long spaceId,
                           Authentication authentication, HttpServletRequest request) {
        assignRoleToUserService.execute(userId, id, spaceId);
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("@securityService.userHasPermissionForRole(authentication, #request, #id)")
    public List<RoleEndpointPermissionResponse> getPermissions(@PathVariable Long id,
                                                                Authentication authentication,
                                                                HttpServletRequest request) {
        return getRolePermissionsService.execute(id, authentication.getName());
    }

    @PatchMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("@securityService.userHasPermissionForRole(authentication, #request, #id)")
    public RoleEndpointPermissionResponse updatePermissionAccess(@PathVariable Long id, @PathVariable Long permissionId,
                                                                  @RequestBody UpdateRolePermissionAccessRequest body,
                                                                  Authentication authentication, HttpServletRequest request) {
        return updateRolePermissionAccessService.execute(id, permissionId, body);
    }
}
