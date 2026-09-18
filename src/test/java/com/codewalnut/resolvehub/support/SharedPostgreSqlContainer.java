
package com.codewalnut.resolvehub.support;

import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgreSqlContainer {

    public static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("resolvehub")
                    .withUsername("resolvehub")
                    .withPassword("resolvehub-test");

    static {
        INSTANCE.start();
    }

    private SharedPostgreSqlContainer() {
    }
}
