
package com.codewalnut.resolvehub.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class PostgreSqlTestSupport {

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgreSqlContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedPostgreSqlContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedPostgreSqlContainer.INSTANCE::getPassword);
    }
}
