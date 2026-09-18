package com.codewalnut.resolvehub.dto;

import java.util.Set;
import java.util.UUID;
import com.codewalnut.resolvehub.domain.ApplicationRole;

public record UserResponse(UUID id, String username, boolean enabled, Set<ApplicationRole> roles) {
    public UserResponse {
        roles = Set.copyOf(roles);
    }
}
