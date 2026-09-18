package com.codewalnut.resolvehub.dto;

import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.CodePointLength;
import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.validation.Utf8ByteLength;

public record CreateUserRequest(
        @NotBlank @CodePointLength(max = 100)
        @Pattern(regexp = "[^:]*", message = "Username must not contain a colon") String username,
        @NotBlank @Utf8ByteLength(max = 72)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") String password,
        @NotEmpty Set<@NotNull ApplicationRole> roles) {
    public CreateUserRequest {
        username = username == null ? null : username.trim();
        roles = roles == null ? null : java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(roles));
    }

    @Override
    public String toString() {
        return "CreateUserRequest[credentials=REDACTED]";
    }
}
