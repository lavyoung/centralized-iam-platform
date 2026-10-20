package com.lavyoung.iam.bootstrap.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 IAM 核心时间与密码学 Bean 的安全默认行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class IamCoreConfigurationTest {

    private final IamCoreConfiguration configuration = new IamCoreConfiguration();

    /**
     * Given 核心配置，When 获取系统时钟，Then 应固定使用 UTC 时区。
     */
    @Test
    void shouldProvideUtcSystemClock() {
        Clock clock = configuration.systemClock();

        assertTrue(clock.getZone().getRules().isFixedOffset());
        assertTrue(clock.getZone().getRules().getOffset(clock.instant()).equals(ZoneOffset.UTC));
    }

    /**
     * Given 核心配置，When 编码并验证密钥，Then 摘要应携带算法标识且不可逆展示明文。
     */
    @Test
    void shouldProvideDelegatingPasswordEncoder() {
        PasswordEncoder passwordEncoder = configuration.passwordEncoder();
        String rawSecret = "client-secret-value";

        String encodedSecret = passwordEncoder.encode(rawSecret);

        assertAll(
                () -> assertTrue(encodedSecret.startsWith("{")),
                () -> assertNotEquals(rawSecret, encodedSecret),
                () -> assertTrue(passwordEncoder.matches(rawSecret, encodedSecret))
        );
    }
}
