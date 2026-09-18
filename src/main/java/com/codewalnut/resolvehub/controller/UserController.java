package com.codewalnut.resolvehub.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.codewalnut.resolvehub.dto.CreateUserRequest;
import com.codewalnut.resolvehub.dto.UserResponse;
import com.codewalnut.resolvehub.service.UserService;

@RestController
@Tag(name = "Users", description = "Administrator-only user provisioning")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping("/api/v1/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user (ADMIN only)")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return users.create(request);
    }
}
