package com.lavyoung.iam.bootstrap.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证公共运行配置及 MySQL、PostgreSQL profile 的数据库隔离约束。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
class IamDatabaseProfileConfigurationTest {

    /**
     * Given 公共配置，When 读取数据源凭据，Then 应只引用环境变量且不得提交默认密码。
     */
    @Test
    void shouldRequireDatabaseCredentialsFromEnvironment() {
        // Given / When
        Properties properties = load("application.yml");

        // Then
        assertAll(
                () -> assertEquals("${IAM_DATASOURCE_USERNAME}", properties.getProperty("spring.datasource.username")),
                () -> assertEquals("${IAM_DATASOURCE_PASSWORD}", properties.getProperty("spring.datasource.password")),
                () -> assertEquals("mysql", properties.getProperty("spring.profiles.default"))
        );
    }

    /**
     * Given MySQL profile，When 读取迁移位置，Then 只能组合公共与 MySQL 脚本。
     */
    @Test
    void shouldIsolateMySqlMigrationLocations() {
        // Given / When
        Properties properties = load("application-mysql.yml");

        // Then
        assertAll(
                () -> assertEquals("com.mysql.cj.jdbc.Driver", properties.getProperty("spring.datasource.driver-class-name")),
                () -> assertEquals("classpath:db/migration/common", properties.getProperty("spring.flyway.locations[0]")),
                () -> assertEquals("classpath:db/migration/mysql", properties.getProperty("spring.flyway.locations[1]")),
                () -> assertFalse(properties.containsValue("classpath:db/migration/postgresql"))
        );
    }

    /**
     * Given PostgreSQL profile，When 读取迁移位置，Then 只能组合公共与 PostgreSQL 脚本。
     */
    @Test
    void shouldIsolatePostgreSqlMigrationLocations() {
        // Given / When
        Properties properties = load("application-postgresql.yml");

        // Then
        assertAll(
                () -> assertEquals("org.postgresql.Driver", properties.getProperty("spring.datasource.driver-class-name")),
                () -> assertEquals("classpath:db/migration/common", properties.getProperty("spring.flyway.locations[0]")),
                () -> assertEquals("classpath:db/migration/postgresql", properties.getProperty("spring.flyway.locations[1]")),
                () -> assertFalse(properties.containsValue("classpath:db/migration/mysql"))
        );
    }

    private Properties load(String resourcePath) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourcePath));
        return Objects.requireNonNull(factory.getObject(), resourcePath);
    }
}
