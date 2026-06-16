package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.menu.GetMenuStructureService;
import com.devhouse.financial_plan.application.menu.dto.GroupMenuStructureDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menu-structure")
public class MenuStructureController {

    private final GetMenuStructureService getMenuStructureService;

    public MenuStructureController(GetMenuStructureService getMenuStructureService) {
        this.getMenuStructureService = getMenuStructureService;
    }

    @GetMapping
    public List<GroupMenuStructureDto> getMenuStructure(@RequestParam Long spaceId, Authentication authentication) {
        return getMenuStructureService.execute(authentication.getName(), spaceId);
    }
}
