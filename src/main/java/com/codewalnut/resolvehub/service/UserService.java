package com.codewalnut.resolvehub.service;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codewalnut.resolvehub.dto.CreateUserRequest;
import com.codewalnut.resolvehub.dto.UserResponse;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.repository.AppUserRepository;
import com.codewalnut.resolvehub.repository.RoleRepository;

@Service
public class UserService {
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final Clock clock;

    public UserService(AppUserRepository users, RoleRepository roles, PasswordEncoder passwords, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(CreateUserRequest request) {
        var assignedRoles = request.roles().stream()
                .map(role -> roles.findByName(role).orElseThrow()).collect(Collectors.toSet());
        var user = users.saveAndFlush(new AppUserEntity(UUID.randomUUID(), request.username(),
                passwords.encode(request.password()), true, clock.instant(), assignedRoles));
        return new UserResponse(user.getId(), user.getUsername(), user.isEnabled(), request.roles());
    }
}
