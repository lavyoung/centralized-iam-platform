package com.lavyoung.iam.adapter.inbound.rest.clientregistration.assembler;

import com.lavyoung.iam.application.clientregistration.command.RegisterClientCommand;
import com.lavyoung.iam.contract.clientregistration.request.RegisterConfidentialClientRequest;
import com.lavyoung.iam.contract.clientregistration.request.RegisterPublicClientRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端注册请求到应用命令的 MapStruct 转换与非空边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
class ClientRegistrationRequestAssemblerTest {

    private static final String TENANT_ID = "default";
    private static final String TEST_SECRET = "x".repeat(32);

    private final ClientRegistrationRequestAssembler assembler =
            Mappers.getMapper(ClientRegistrationRequestAssembler.class);

    /**
     * Given 公共客户端请求和可信租户，When 执行装配，Then 应完整转换为应用命令。
     */
    @Test
    void shouldMapPublicRequestToCommand() {
        // Given
        RegisterPublicClientRequest request = publicRequest();

        // When
        RegisterClientCommand command = assembler.toCommand(request, TENANT_ID);

        // Then
        assertAll(
                () -> assertEquals(TENANT_ID, command.tenantId()),
                () -> assertEquals(request.clientId(), command.clientId()),
                () -> assertEquals(request.authenticationMethods(), command.authenticationMethods()),
                () -> assertEquals(Duration.ofSeconds(900), command.accessTokenTimeToLive()),
                () -> assertEquals(Duration.ofSeconds(28_800), command.refreshTokenTimeToLive()),
                () -> assertEquals(Duration.ofSeconds(300), command.authorizationCodeTimeToLive())
        );
    }

    /**
     * Given 含明文密钥的机密客户端请求，When 执行装配，Then 命令不得携带密钥。
     */
    @Test
    void shouldMapConfidentialRequestWithoutSecret() {
        // Given
        RegisterConfidentialClientRequest request = confidentialRequest();

        // When
        RegisterClientCommand command = assembler.toCommand(request, TENANT_ID);

        // Then
        assertAll(
                () -> assertEquals("service-app", command.clientId()),
                () -> assertEquals(Set.of("client_secret_basic"), command.authenticationMethods()),
                () -> assertEquals(Set.of("client_credentials"), command.grantTypes()),
                () -> assertFalse(command.toString().contains(TEST_SECRET))
        );
    }

    /**
     * Given 请求或可信租户为空，When 执行装配，Then 应立即拒绝而不是返回空值。
     */
    @Test
    void shouldRejectMissingRequestOrTenant() {
        // When / Then
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> assembler.toCommand((RegisterPublicClientRequest) null, TENANT_ID)),
                () -> assertThrows(NullPointerException.class,
                        () -> assembler.toCommand(publicRequest(), null))
        );
    }

    private RegisterPublicClientRequest publicRequest() {
        return new RegisterPublicClientRequest(
                "public-web-app",
                "Public Web App",
                Set.of("none"),
                Set.of("authorization_code", "refresh_token"),
                Set.of("https://app.example.com/callback"),
                Set.of("openid", "profile"),
                900,
                28_800,
                300
        );
    }

    private RegisterConfidentialClientRequest confidentialRequest() {
        return new RegisterConfidentialClientRequest(
                "service-app",
                "Service App",
                Set.of("client_secret_basic"),
                Set.of("client_credentials"),
                Set.of(),
                Set.of("service.read"),
                900,
                28_800,
                300,
                TEST_SECRET
        );
    }
}
