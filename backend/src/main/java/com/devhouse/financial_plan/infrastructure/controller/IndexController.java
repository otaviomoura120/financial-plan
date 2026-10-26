package com.devhouse.financial_plan.infrastructure.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @GetMapping("/")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public String index(Authentication authentication, HttpServletRequest request) {
        return "Hello World!" + authentication.getName();
    }
}
