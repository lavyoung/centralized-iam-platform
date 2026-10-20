package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端注册聚合的客户端类型、状态流转和密钥轮换规则。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientRegistrationTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-10-20T08:00:00Z");

    /**
     * Given 公共客户端配置，When 注册客户端，Then 应启用客户端且不保存密钥摘要。
     */
    @Test
    void shouldRegisterPublicClient() {
        ClientRegistration registration = ClientRegistration.registerPublic(publicSpecification(), REGISTERED_AT);

        assertAll(
                () -> assertEquals(ClientStatus.ACTIVE, registration.status()),
                () -> assertTrue(registration.allowsAuthentication()),
                () -> assertTrue(registration.secretHash().isEmpty()),
                () -> assertTrue(registration.supportsGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)),
                () -> assertFalse(registration.supportsGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)),
                () -> assertEquals(REGISTERED_AT, registration.registeredAt()),
                () -> assertEquals(REGISTERED_AT, registration.updatedAt())
        );
    }

    /**
     * Given 公共客户端不允许的认证或授权方式，When 注册客户端，Then 应拒绝配置。
     */
    @Test
    void shouldRejectInvalidPublicClient() {
        assertAll(
                () -> assertInvalid(() -> ClientRegistration.registerPublic(confidentialSpecification(), REGISTERED_AT)),
                () -> assertInvalid(() -> ClientRegistration.registerPublic(publicClientCredentialsSpecification(), REGISTERED_AT))
        );
    }

    /**
     * Given 机密客户端配置和密钥摘要，When 注册并轮换密钥，Then 应保存最新摘要和变更时间。
     */
    @Test
    void shouldRegisterConfidentialClientAndRotateSecret() {
        ClientSecretHash originalSecret = new ClientSecretHash("a".repeat(60));
        ClientSecretHash rotatedSecret = new ClientSecretHash("b".repeat(60));
        ClientRegistration registration = ClientRegistration.registerConfidential(confidentialSpecification(), originalSecret, REGISTERED_AT);
        Instant rotatedAt = REGISTERED_AT.plusSeconds(60);

        registration.rotateSecret(rotatedSecret, rotatedAt);

        assertAll(
                () -> assertEquals(rotatedSecret, registration.secretHash().orElseThrow()),
                () -> assertEquals(rotatedAt, registration.updatedAt()),
                () -> assertTrue(registration.allowsAuthentication())
        );
    }

    /**
     * Given 已停用公共客户端的持久化状态，When 恢复聚合，Then 应保留历史状态且不产生密钥摘要。
     */
    @Test
    void shouldRestoreDisabledPublicClient() {
        Instant updatedAt = REGISTERED_AT.plusSeconds(60);
        ClientLifecycleState lifecycleState = new ClientLifecycleState(ClientStatus.DISABLED, REGISTERED_AT, updatedAt);

        ClientRegistration registration = ClientRegistration.restorePublic(publicSpecification(), lifecycleState);

        assertAll(
                () -> assertEquals(ClientStatus.DISABLED, registration.status()),
                () -> assertFalse(registration.allowsAuthentication()),
                () -> assertTrue(registration.secretHash().isEmpty()),
                () -> assertEquals(REGISTERED_AT, registration.registeredAt()),
                () -> assertEquals(updatedAt, registration.updatedAt())
        );
    }

    /**
     * Given 机密客户端的持久化状态和密钥摘要，When 恢复聚合，Then 应保留密钥和生命周期。
     */
    @Test
    void shouldRestoreConfidentialClient() {
        ClientSecretHash secretHash = new ClientSecretHash("a".repeat(60));
        Instant updatedAt = REGISTERED_AT.plusSeconds(60);
        ClientLifecycleState lifecycleState = new ClientLifecycleState(ClientStatus.ACTIVE, REGISTERED_AT, updatedAt);

        ClientRegistration registration = ClientRegistration.restoreConfidential(confidentialSpecification(), secretHash, lifecycleState);

        assertAll(
                () -> assertEquals(secretHash, registration.secretHash().orElseThrow()),
                () -> assertEquals(ClientStatus.ACTIVE, registration.status()),
                () -> assertEquals(REGISTERED_AT, registration.registeredAt()),
                () -> assertEquals(updatedAt, registration.updatedAt())
        );
    }

    /**
     * Given 与客户端类型冲突的持久化数据，When 恢复聚合，Then 应拒绝绕过注册规则。
     */
    @Test
    void shouldRejectInvalidPersistedClientState() {
        ClientLifecycleState lifecycleState = ClientLifecycleState.initial(REGISTERED_AT);

        assertAll(
                () -> assertInvalid(() -> ClientRegistration.restorePublic(confidentialSpecification(), lifecycleState)),
                () -> assertInvalid(() -> ClientRegistration.restorePublic(publicClientCredentialsSpecification(), lifecycleState)),
                () -> assertInvalid(() -> ClientRegistration.restoreConfidential(publicSpecification(), new ClientSecretHash("a".repeat(60)), lifecycleState)),
                () -> assertInvalid(() -> ClientRegistration.restoreConfidential(confidentialSpecification(), null, lifecycleState)),
                () -> assertInvalid(() -> ClientRegistration.restorePublic(publicSpecification(), null))
        );
    }

    /**
     * Given 有效客户端，When 停用后重新启用，Then 应更新认证状态和最后变更时间。
     */
    @Test
    void shouldDisableAndReactivateClient() {
        ClientRegistration registration = ClientRegistration.registerPublic(publicSpecification(), REGISTERED_AT);
        Instant disabledAt = REGISTERED_AT.plusSeconds(60);
        Instant activatedAt = REGISTERED_AT.plusSeconds(120);

        registration.disable(disabledAt);

        assertAll(
                () -> assertEquals(ClientStatus.DISABLED, registration.status()),
                () -> assertFalse(registration.allowsAuthentication()),
                () -> assertEquals(disabledAt, registration.updatedAt())
        );

        registration.activate(activatedAt);

        assertAll(
                () -> assertEquals(ClientStatus.ACTIVE, registration.status()),
                () -> assertTrue(registration.allowsAuthentication()),
                () -> assertEquals(activatedAt, registration.updatedAt())
        );
    }

    /**
     * Given 公共客户端或早于当前状态的时间，When 修改聚合，Then 应拒绝非法变更。
     */
    @Test
    void shouldRejectInvalidAggregateChanges() {
        ClientRegistration registration = ClientRegistration.registerPublic(publicSpecification(), REGISTERED_AT);

        assertAll(
                () -> assertInvalid(() -> registration.rotateSecret(new ClientSecretHash("a".repeat(60)), REGISTERED_AT.plusSeconds(1))),
                () -> assertInvalid(() -> registration.disable(REGISTERED_AT.minusSeconds(1))),
                () -> assertInvalid(() -> registration.activate(null))
        );
    }

    private ClientRegistrationSpec publicSpecification() {
        return new ClientRegistrationSpec(
                new TenantId("default"),
                new ClientId("public-web-app"),
                "Public Web App",
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN),
                Set.of(new RedirectUri("https://app.example.com/callback")),
                Set.of("openid", "profile"),
                ClientTokenPolicy.standard()
        );
    }

    private ClientRegistrationSpec confidentialSpecification() {
        return new ClientRegistrationSpec(
                new TenantId("default"),
                new ClientId("service-app"),
                "Service App",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                Set.of(),
                Set.of("service.read"),
                ClientTokenPolicy.standard()
        );
    }

    private ClientRegistrationSpec publicClientCredentialsSpecification() {
        return new ClientRegistrationSpec(
                new TenantId("default"),
                new ClientId("invalid-public-app"),
                "Invalid Public App",
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                Set.of(),
                Set.of("service.read"),
                ClientTokenPolicy.standard()
        );
    }

    private void assertInvalid(Executable action) {
        IamException exception = assertThrows(IamException.class, action);

        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, exception.getErrorCode());
    }
}
