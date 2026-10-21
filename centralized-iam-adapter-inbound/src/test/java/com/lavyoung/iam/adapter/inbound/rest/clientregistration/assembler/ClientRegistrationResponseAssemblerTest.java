package com.lavyoung.iam.adapter.inbound.rest.clientregistration.assembler;

import com.lavyoung.iam.application.clientregistration.result.RegisteredClientResult;
import com.lavyoung.iam.contract.clientregistration.response.ClientRegistrationResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端注册应用结果到响应契约的 MapStruct 转换与非空边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
class ClientRegistrationResponseAssemblerTest {

    private final ClientRegistrationResponseAssembler assembler =
            Mappers.getMapper(ClientRegistrationResponseAssembler.class);

    /**
     * Given 完整应用结果，When 执行装配，Then 应将时间长度转换为秒并保留业务字段。
     */
    @Test
    void shouldMapResultToResponse() {
        // Given
        RegisteredClientResult result = registeredClientResult();

        // When
        ClientRegistrationResponse response = assembler.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(result.tenantId(), response.tenantId()),
                () -> assertEquals(result.clientId(), response.clientId()),
                () -> assertEquals(result.authenticationMethods(), response.authenticationMethods()),
                () -> assertEquals(900, response.accessTokenTtlSeconds()),
                () -> assertEquals(28_800, response.refreshTokenTtlSeconds()),
                () -> assertEquals(300, response.authorizationCodeTtlSeconds()),
                () -> assertEquals("ACTIVE", response.status())
        );
    }

    /**
     * Given 空应用结果，When 执行装配，Then 应立即拒绝而不是返回空响应。
     */
    @Test
    void shouldRejectMissingResult() {
        // When / Then
        assertThrows(NullPointerException.class, () -> assembler.toResponse(null));
    }

    private RegisteredClientResult registeredClientResult() {
        Instant registeredAt = Instant.parse("2026-10-20T08:00:00Z");
        return new RegisteredClientResult(
                "default",
                "public-web-app",
                "Public Web App",
                Set.of("none"),
                Set.of("authorization_code", "refresh_token"),
                Set.of("https://app.example.com/callback"),
                Set.of("openid", "profile"),
                Duration.ofMinutes(15),
                Duration.ofHours(8),
                Duration.ofMinutes(5),
                "ACTIVE",
                registeredAt,
                registeredAt
        );
    }
}
