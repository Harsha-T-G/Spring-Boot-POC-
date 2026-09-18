package com.codewalnut.resolvehub.entity;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private ApplicationRole name;

    protected RoleEntity() {
    }

    public RoleEntity(UUID id, ApplicationRole name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public ApplicationRole getName() {
        return name;
    }
}
