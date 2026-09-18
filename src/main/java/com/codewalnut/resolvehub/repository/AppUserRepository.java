package com.codewalnut.resolvehub.repository;

import com.codewalnut.resolvehub.entity.AppUserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

    @EntityGraph(attributePaths = "roles")
    @Query("select user from AppUserEntity user where lower(user.username) = lower(:username)")
    Optional<AppUserEntity> findByUsernameIgnoreCase(@Param("username") String username);
}
