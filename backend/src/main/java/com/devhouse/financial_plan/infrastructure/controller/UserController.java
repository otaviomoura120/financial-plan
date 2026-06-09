package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.user.CreateUserService;
import com.devhouse.financial_plan.application.user.DeleteUserService;
import com.devhouse.financial_plan.application.user.UpdateUserService;
import com.devhouse.financial_plan.application.user.dto.CreateUserRequest;
import com.devhouse.financial_plan.application.user.dto.UpdateUserRequest;
import com.devhouse.financial_plan.application.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final CreateUserService createUserService;
    private final UpdateUserService updateUserService;
    private final DeleteUserService deleteUserService;

    public UserController(CreateUserService createUserService, UpdateUserService updateUserService, DeleteUserService deleteUserService) {
        this.createUserService = createUserService;
        this.updateUserService = updateUserService;
        this.deleteUserService = deleteUserService;
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
}
