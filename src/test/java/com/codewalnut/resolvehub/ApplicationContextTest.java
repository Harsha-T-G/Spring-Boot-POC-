
package com.codewalnut.resolvehub;

import com.codewalnut.resolvehub.support.PostgreSqlTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest extends PostgreSqlTestSupport {

    @Test
    void givenPostgreSqlTestContainer_whenApplicationStarts_thenContextLoads() {
    }
}
