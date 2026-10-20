package com.lavyoung.iam.adapter.outbound.persistence.clientregistration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端注册数据库迁移资源的打包与核心租户约束。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientRegistrationMigrationResourcesTest {

    private static final List<String> TABLE_NAMES = List.of(
            "iam_client_registration",
            "iam_client_authentication_method",
            "iam_client_authorization_grant_type",
            "iam_client_redirect_uri",
            "iam_client_scope"
    );

    /**
     * Given 两种数据库方言，When 读取迁移资源，Then 均应包含完整表结构和租户边界约束。
     *
     * @throws IOException 当迁移资源无法读取时抛出
     */
    @Test
    void shouldPackageDialectMigrationsWithTenantBoundaries() throws IOException {
        assertAll(
                () -> assertMigration("mysql"),
                () -> assertMigration("postgresql")
        );
    }

    private void assertMigration(String dialect) throws IOException {
        String resourcePath = "db/migration/%s/V1__create_client_registration_schema.sql".formatted(dialect);

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(stream, () -> "Missing migration resource: " + resourcePath);
            String migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertAll(
                    TABLE_NAMES.stream()
                            .<Executable>map(table -> () -> assertTrue(migration.contains("CREATE TABLE " + table)))
                            .toList()
            );
            assertTrue(migration.contains("UNIQUE (tenant_id, client_id)"));
            assertTrue(migration.contains("FOREIGN KEY (tenant_id, client_registration_id)"));
            assertTrue(migration.contains("UNIQUE (tenant_id, client_registration_id, redirect_uri_hash)"));
        }
    }
}
