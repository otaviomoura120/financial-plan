package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.endpointpermission.CreateEndpointPermissionService;
import com.devhouse.financial_plan.application.endpointpermission.DeleteEndpointPermissionService;
import com.devhouse.financial_plan.application.endpointpermission.GetEndpointPermissionsService;
import com.devhouse.financial_plan.application.endpointpermission.UpdateEndpointPermissionService;
import com.devhouse.financial_plan.application.endpointpermission.dto.CreateEndpointPermissionRequest;
import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse;
import com.devhouse.financial_plan.application.endpointpermission.dto.UpdateEndpointPermissionRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/endpoint-permissions")
public class EndpointPermissionController {

    private final CreateEndpointPermissionService createEndpointPermissionService;
    private final UpdateEndpointPermissionService updateEndpointPermissionService;
    private final DeleteEndpointPermissionService deleteEndpointPermissionService;
    private final GetEndpointPermissionsService getEndpointPermissionsService;

    public EndpointPermissionController(CreateEndpointPermissionService createEndpointPermissionService,
                                        UpdateEndpointPermissionService updateEndpointPermissionService,
                                        DeleteEndpointPermissionService deleteEndpointPermissionService,
                                        GetEndpointPermissionsService getEndpointPermissionsService) {
        this.createEndpointPermissionService = createEndpointPermissionService;
        this.updateEndpointPermissionService = updateEndpointPermissionService;
        this.deleteEndpointPermissionService = deleteEndpointPermissionService;
        this.getEndpointPermissionsService = getEndpointPermissionsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public EndpointPermissionResponse create(@RequestBody CreateEndpointPermissionRequest body, Authentication authentication, HttpServletRequest request) {
        return createEndpointPermissionService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public EndpointPermissionResponse update(@PathVariable Long id, @RequestBody UpdateEndpointPermissionRequest body, Authentication authentication, HttpServletRequest request) {
        return updateEndpointPermissionService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteEndpointPermissionService.execute(id);
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<EndpointPermissionResponse> getAll(@RequestParam(required = false) String group, Authentication authentication, HttpServletRequest request) {
        return getEndpointPermissionsService.execute(group);
    }
}
