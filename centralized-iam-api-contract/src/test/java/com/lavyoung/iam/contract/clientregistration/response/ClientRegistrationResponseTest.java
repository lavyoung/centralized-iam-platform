package com.lavyoung.iam.contract.clientregistration.response;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证客户端注册响应不会暴露可变集合。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientRegistrationResponseTest {

    /**
     * Given 可变权限集合，When 创建响应后修改原集合，Then 响应内容应保持不变。
     */
    @Test
    void shouldDefensivelyCopyCollectionValues() {
        // Given
        Set<String> scopes = new HashSet<>(Set.of("openid"));

        // When
        ClientRegistrationResponse response = response(scopes);
        scopes.add("profile");

        // Then
        assertEquals(Set.of("openid"), response.scopes());
        assertThrows(UnsupportedOperationException.class, () -> response.scopes().add("email"));
    }

    private ClientRegistrationResponse response(Set<String> scopes) {
        Instant registeredAt = Instant.parse("2026-10-20T08:00:00Z");
        return new ClientRegistrationResponse(
                "default",
                "public-web-app",
                "Public Web App",
                Set.of("none"),
                Set.of("authorization_code", "refresh_token"),
                Set.of("https://app.example.com/callback"),
                scopes,
                900,
                28_800,
                300,
                "ACTIVE",
                registeredAt,
                registeredAt
        );
    }
}
