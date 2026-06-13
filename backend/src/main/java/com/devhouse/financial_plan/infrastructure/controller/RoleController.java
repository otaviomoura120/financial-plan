package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.role.AssignRoleToUserService;
import com.devhouse.financial_plan.application.role.CreateRoleService;
import com.devhouse.financial_plan.application.role.DeleteRoleService;
import com.devhouse.financial_plan.application.role.GetRolesBySpaceService;
import com.devhouse.financial_plan.application.role.UpdateRoleService;
import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest;
import com.devhouse.financial_plan.application.role.dto.RoleResponse;
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

    public RoleController(CreateRoleService createRoleService, UpdateRoleService updateRoleService,
                          DeleteRoleService deleteRoleService, GetRolesBySpaceService getRolesBySpaceService,
                          AssignRoleToUserService assignRoleToUserService) {
        this.createRoleService = createRoleService;
        this.updateRoleService = updateRoleService;
        this.deleteRoleService = deleteRoleService;
        this.getRolesBySpaceService = getRolesBySpaceService;
        this.assignRoleToUserService = assignRoleToUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public RoleResponse create(@RequestBody CreateRoleRequest body, Authentication authentication, HttpServletRequest request) {
        return createRoleService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public RoleResponse update(@PathVariable Long id, @RequestBody UpdateRoleRequest body, Authentication authentication, HttpServletRequest request) {
        return updateRoleService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteRoleService.execute(id);
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<RoleResponse> getBySpace(@RequestParam Long spaceId, Authentication authentication, HttpServletRequest request) {
        return getRolesBySpaceService.execute(spaceId);
    }

    @PutMapping("/{id}/assign-user/{userId}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void assignRole(@PathVariable Long id, @PathVariable Long userId, @RequestParam Long spaceId,
                           Authentication authentication, HttpServletRequest request) {
        assignRoleToUserService.execute(userId, id, spaceId);
    }
}
