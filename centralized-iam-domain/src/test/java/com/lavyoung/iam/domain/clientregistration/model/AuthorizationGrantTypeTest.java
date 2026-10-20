package com.lavyoung.iam.domain.clientregistration.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证授权类型与 OAuth2 协议值之间的稳定映射。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class AuthorizationGrantTypeTest {

    /**
     * Given 所有受支持的授权类型，When 使用协议值解析，Then 应恢复对应枚举。
     */
    @Test
    void shouldResolveSupportedProtocolValues() {
        assertAll(
                () -> assertResolved(AuthorizationGrantType.AUTHORIZATION_CODE),
                () -> assertResolved(AuthorizationGrantType.CLIENT_CREDENTIALS),
                () -> assertResolved(AuthorizationGrantType.REFRESH_TOKEN)
        );
    }

    /**
     * Given 已废弃、未知或空协议值，When 解析授权类型，Then 应返回空结果。
     */
    @Test
    void shouldReturnEmptyForUnsupportedProtocolValue() {
        assertAll(
                () -> assertTrue(AuthorizationGrantType.fromProtocolValue("password").isEmpty()),
                () -> assertTrue(AuthorizationGrantType.fromProtocolValue("").isEmpty()),
                () -> assertTrue(AuthorizationGrantType.fromProtocolValue(null).isEmpty())
        );
    }

    private void assertResolved(AuthorizationGrantType grantType) {
        Optional<AuthorizationGrantType> resolved =
                AuthorizationGrantType.fromProtocolValue(grantType.getCode());

        assertEquals(Optional.of(grantType), resolved);
    }
}
