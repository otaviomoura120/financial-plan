package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.user.CreateUserService;
import com.devhouse.financial_plan.application.user.DeleteUserService;
import com.devhouse.financial_plan.application.user.FindUserByAuth0SubService;
import com.devhouse.financial_plan.application.user.FindUserByEmailService;
import com.devhouse.financial_plan.application.user.UpdateUserService;
import com.devhouse.financial_plan.application.user.dto.CreateUserRequest;
import com.devhouse.financial_plan.application.user.dto.UpdateUserRequest;
import com.devhouse.financial_plan.application.user.dto.UserMeResponse;
import com.devhouse.financial_plan.application.user.dto.UserResponse;
import com.devhouse.financial_plan.application.user.dto.UserSearchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final CreateUserService createUserService;
    private final UpdateUserService updateUserService;
    private final DeleteUserService deleteUserService;
    private final FindUserByAuth0SubService findUserByAuth0SubService;
    private final FindUserByEmailService findUserByEmailService;

    public UserController(CreateUserService createUserService, UpdateUserService updateUserService,
                          DeleteUserService deleteUserService, FindUserByAuth0SubService findUserByAuth0SubService,
                          FindUserByEmailService findUserByEmailService) {
        this.createUserService = createUserService;
        this.updateUserService = updateUserService;
        this.deleteUserService = deleteUserService;
        this.findUserByAuth0SubService = findUserByAuth0SubService;
        this.findUserByEmailService = findUserByEmailService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(Authentication authentication) {
        UserMeResponse response = findUserByAuth0SubService.execute(authentication.getName());
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody CreateUserRequest request) {
        return createUserService.execute(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return updateUserService.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUserService.execute(id);
    }

    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> search(@RequestParam String email) {
        return findUserByEmailService.execute(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
