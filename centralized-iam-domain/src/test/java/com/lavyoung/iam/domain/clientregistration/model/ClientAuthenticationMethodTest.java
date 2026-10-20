package com.lavyoung.iam.domain.clientregistration.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端认证方式与 OAuth2/OIDC 协议值之间的稳定映射。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientAuthenticationMethodTest {

    /**
     * Given 所有受支持的客户端认证方式，When 使用协议值解析，Then 应恢复对应枚举。
     */
    @Test
    void shouldResolveSupportedProtocolValues() {
        assertAll(
                () -> assertResolved(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                () -> assertResolved(ClientAuthenticationMethod.CLIENT_SECRET_POST),
                () -> assertResolved(ClientAuthenticationMethod.NONE)
        );
    }

    /**
     * Given 未知或空协议值，When 解析客户端认证方式，Then 应返回空结果。
     */
    @Test
    void shouldReturnEmptyForUnsupportedProtocolValue() {
        assertAll(
                () -> assertTrue(ClientAuthenticationMethod.fromMethod("private_key_jwt").isEmpty()),
                () -> assertTrue(ClientAuthenticationMethod.fromMethod("").isEmpty()),
                () -> assertTrue(ClientAuthenticationMethod.fromMethod(null).isEmpty())
        );
    }

    private void assertResolved(ClientAuthenticationMethod method) {
        Optional<ClientAuthenticationMethod> resolved = ClientAuthenticationMethod.fromMethod(method.getMethod());

        assertEquals(Optional.of(method), resolved);
    }
}
