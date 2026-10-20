package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;

/**
 * 在真实 PostgreSQL 容器中验证客户端注册迁移及数据库约束。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlClientRegistrationMigrationIT {

    @Container
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16.2")
            .withDatabaseName("centralized_iam");

    /**
     * Given 一个空 PostgreSQL 数据库，When 执行迁移，Then 表结构与租户约束应可用。
     *
     * @throws SQLException 当 JDBC 验证失败时抛出
     */
    @Test
    void shouldMigrateClientRegistrationSchema() throws SQLException {
        ClientRegistrationMigrationTestSupport.verify(DATABASE, "classpath:db/migration/postgresql");
    }
}
