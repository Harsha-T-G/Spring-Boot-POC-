package com.codewalnut.resolvehub.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.repository.AppUserRepository;
import com.codewalnut.resolvehub.repository.RoleRepository;

@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "resolvehub.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevelopmentUserSeeder implements ApplicationRunner {
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final Environment environment;

    public DevelopmentUserSeeder(AppUserRepository users, RoleRepository roles, PasswordEncoder encoder,
            Clock clock, Environment environment) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.clock = clock;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        var accounts = List.of(new SeedAccount("customer", "CUSTOMER", ApplicationRole.CUSTOMER),
                new SeedAccount("agent-one", "AGENT_ONE", ApplicationRole.SUPPORT_AGENT),
                new SeedAccount("agent-two", "AGENT_TWO", ApplicationRole.SUPPORT_AGENT),
                new SeedAccount("admin", "ADMIN", ApplicationRole.ADMIN));
        var missing = accounts.stream().filter(account -> users.findByUsernameIgnoreCase(account.username()).isEmpty())
                .toList();
        missing.forEach(this::password);
        for (var account : missing) {
            var role = roles.findByName(account.role()).orElseThrow(() -> new IllegalStateException("Seed role missing"));
            users.save(new AppUserEntity(UUID.randomUUID(), account.username(), encoder.encode(password(account)),
                    true, clock.instant(), Set.of(role)));
        }
    }

    private String password(SeedAccount account) {
        String key = "RESOLVEHUB_" + account.passwordKey() + "_PASSWORD";
        String value = environment.getProperty(key);
        if (value == null || value.isBlank() || value.equals("replace-with-a-local-password")
                || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Set " + key + " to a nonblank password of at most 72 UTF-8 bytes");
        }
        return value;
    }

    private record SeedAccount(String username, String passwordKey, ApplicationRole role) {
    }
}
