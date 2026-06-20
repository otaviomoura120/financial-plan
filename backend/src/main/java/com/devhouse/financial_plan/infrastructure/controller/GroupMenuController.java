package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.groupmenu.AddGroupMenuChildrenService;
import com.devhouse.financial_plan.application.groupmenu.CreateGroupMenuService;
import com.devhouse.financial_plan.application.groupmenu.DeleteGroupMenuService;
import com.devhouse.financial_plan.application.groupmenu.GetGroupMenuService;
import com.devhouse.financial_plan.application.groupmenu.RemoveGroupMenuChildrenService;
import com.devhouse.financial_plan.application.groupmenu.UpdateGroupMenuChildrenService;
import com.devhouse.financial_plan.application.groupmenu.UpdateGroupMenuService;
import com.devhouse.financial_plan.application.groupmenu.dto.CreateGroupMenuChildrenRequest;
import com.devhouse.financial_plan.application.groupmenu.dto.CreateGroupMenuRequest;
import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuChildrenResponse;
import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuResponse;
import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuWithChildrenResponse;
import com.devhouse.financial_plan.application.groupmenu.dto.UpdateGroupMenuChildrenRequest;
import com.devhouse.financial_plan.application.groupmenu.dto.UpdateGroupMenuRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/group-menus")
public class GroupMenuController {

    private final CreateGroupMenuService createGroupMenuService;
    private final UpdateGroupMenuService updateGroupMenuService;
    private final DeleteGroupMenuService deleteGroupMenuService;
    private final GetGroupMenuService getGroupMenuService;
    private final AddGroupMenuChildrenService addGroupMenuChildrenService;
    private final UpdateGroupMenuChildrenService updateGroupMenuChildrenService;
    private final RemoveGroupMenuChildrenService removeGroupMenuChildrenService;

    public GroupMenuController(CreateGroupMenuService createGroupMenuService,
                               UpdateGroupMenuService updateGroupMenuService,
                               DeleteGroupMenuService deleteGroupMenuService,
                               GetGroupMenuService getGroupMenuService,
                               AddGroupMenuChildrenService addGroupMenuChildrenService,
                               UpdateGroupMenuChildrenService updateGroupMenuChildrenService,
                               RemoveGroupMenuChildrenService removeGroupMenuChildrenService) {
        this.createGroupMenuService = createGroupMenuService;
        this.updateGroupMenuService = updateGroupMenuService;
        this.deleteGroupMenuService = deleteGroupMenuService;
        this.getGroupMenuService = getGroupMenuService;
        this.addGroupMenuChildrenService = addGroupMenuChildrenService;
        this.updateGroupMenuChildrenService = updateGroupMenuChildrenService;
        this.removeGroupMenuChildrenService = removeGroupMenuChildrenService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public GroupMenuResponse create(@RequestBody CreateGroupMenuRequest body, Authentication authentication, HttpServletRequest request) {
        return createGroupMenuService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public GroupMenuResponse update(@PathVariable Long id, @RequestBody UpdateGroupMenuRequest body, Authentication authentication, HttpServletRequest request) {
        return updateGroupMenuService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteGroupMenuService.execute(id);
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<GroupMenuWithChildrenResponse> getAll(Authentication authentication, HttpServletRequest request) {
        return getGroupMenuService.execute();
    }

    @PostMapping("/{id}/children")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public GroupMenuChildrenResponse addChild(@PathVariable Long id, @RequestBody CreateGroupMenuChildrenRequest body, Authentication authentication, HttpServletRequest request) {
        CreateGroupMenuChildrenRequest requestWithGroupMenu = new CreateGroupMenuChildrenRequest(id, body.name(), body.endpoint(), body.icon());
        return addGroupMenuChildrenService.execute(requestWithGroupMenu);
    }

    @PutMapping("/children/{childId}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public GroupMenuChildrenResponse updateChild(@PathVariable Long childId, @RequestBody UpdateGroupMenuChildrenRequest body, Authentication authentication, HttpServletRequest request) {
        return updateGroupMenuChildrenService.execute(childId, body);
    }

    @DeleteMapping("/children/{childId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void removeChild(@PathVariable Long childId, Authentication authentication, HttpServletRequest request) {
        removeGroupMenuChildrenService.execute(childId);
    }
}
