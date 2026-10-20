package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.integration;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 为不同数据库方言复用客户端注册迁移和租户约束验证逻辑。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
final class ClientRegistrationMigrationTestSupport {

    private ClientRegistrationMigrationTestSupport() {
    }

    /**
     * 执行指定方言迁移，并验证客户端唯一性、关联表租户边界和长 URI 唯一性。
     *
     * @param database        已启动的数据库容器
     * @param dialectLocation 方言迁移目录
     * @throws SQLException 当 JDBC 验证失败时抛出
     */
    static void verify(JdbcDatabaseContainer<?> database, String dialectLocation) throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                .locations("classpath:db/migration/common", dialectLocation)
                .load();

        assertEquals(1, flyway.migrate().migrationsExecuted);

        try (Connection connection = database.createConnection("")) {
            insertRegistration(connection);
            insertValidAssociations(connection);
            assertDuplicateClientRejected(connection);
            assertCrossTenantAssociationRejected(connection);
            assertDuplicateRedirectUriRejected(connection);
        }
    }

    private static void insertRegistration(Connection connection) throws SQLException {
        execute(connection, """
                INSERT INTO iam_client_registration (
                    id, tenant_id, client_id, display_name, client_secret_hash, status,
                    access_token_ttl_seconds, refresh_token_ttl_seconds, authorization_code_ttl_seconds,
                    registered_at, updated_at
                ) VALUES (
                    1001, 'default', 'console-client', 'Console Client', NULL, 'ACTIVE',
                    900, 28800, 300, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
    }

    private static void insertValidAssociations(Connection connection) throws SQLException {
        execute(connection, """
                INSERT INTO iam_client_authentication_method (
                    id, tenant_id, client_registration_id, authentication_method
                ) VALUES (2001, 'default', 1001, 'none')
                """);
        execute(connection, """
                INSERT INTO iam_client_redirect_uri (
                    id, tenant_id, client_registration_id, redirect_uri
                ) VALUES (3001, 'default', 1001, 'https://client.example/callback')
                """);
    }

    private static void assertDuplicateClientRejected(Connection connection) {
        assertThrows(SQLException.class, () -> execute(connection, """
                INSERT INTO iam_client_registration (
                    id, tenant_id, client_id, display_name, client_secret_hash, status,
                    access_token_ttl_seconds, refresh_token_ttl_seconds, authorization_code_ttl_seconds,
                    registered_at, updated_at
                ) VALUES (
                    1002, 'default', 'console-client', 'Duplicate Client', NULL, 'ACTIVE',
                    900, 28800, 300, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """));
    }

    private static void assertCrossTenantAssociationRejected(Connection connection) {
        assertThrows(SQLException.class, () -> execute(connection, """
                INSERT INTO iam_client_authentication_method (
                    id, tenant_id, client_registration_id, authentication_method
                ) VALUES (2002, 'other', 1001, 'client_secret_post')
                """));
    }

    private static void assertDuplicateRedirectUriRejected(Connection connection) {
        assertThrows(SQLException.class, () -> execute(connection, """
                INSERT INTO iam_client_redirect_uri (
                    id, tenant_id, client_registration_id, redirect_uri
                ) VALUES (3002, 'default', 1001, 'https://client.example/callback')
                """));
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
