package com.devhouse.financial_plan.infrastructure.controller;

import com.auth0.spring.boot.Auth0AuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @GetMapping("/")
    public String index(Authentication authentication) {
        Auth0AuthenticationToken auth0Token = (Auth0AuthenticationToken) authentication;

        return "Hello World!" + auth0Token.getName();
    }
}
