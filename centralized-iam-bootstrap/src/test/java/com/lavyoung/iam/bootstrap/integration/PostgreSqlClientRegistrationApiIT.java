package com.lavyoung.iam.bootstrap.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 在真实 PostgreSQL 容器中验证客户端注册 API 的完整六边形调用链。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlClientRegistrationApiIT extends ClientRegistrationApiITSupport {

    @Container
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16.2")
            .withDatabaseName("centralized_iam");

    /**
     * 将 PostgreSQL 容器连接和方言迁移目录注入 Spring Boot 测试上下文。
     *
     * @param registry 动态属性注册表
     */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
        registry.add("spring.datasource.driver-class-name", DATABASE::getDriverClassName);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/common,classpath:db/migration/postgresql");
    }
}
