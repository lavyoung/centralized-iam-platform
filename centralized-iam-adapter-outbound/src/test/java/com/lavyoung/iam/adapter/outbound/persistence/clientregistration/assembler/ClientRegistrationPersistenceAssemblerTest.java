package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.assembler;

import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.model.ClientRegistrationPersistenceSnapshot;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po.*;
import com.lavyoung.iam.domain.clientregistration.model.*;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端注册聚合与持久化快照之间的双向装配契约。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientRegistrationPersistenceAssemblerTest {

    private static final Long REGISTRATION_ID = 1001L;
    private static final String SECRET_HASH = "{bcrypt}$2a$10$abcdefghijklmnopqrstuv123456789012345678901234567890";
    private static final Instant REGISTERED_AT = Instant.parse("2026-10-20T08:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T09:00:00Z");

    private final ClientRegistrationPersistenceAssembler assembler = new ClientRegistrationPersistenceAssembler();

    /**
     * Given 一个机密客户端聚合，When 转换主表对象，Then 应完整展开单值字段且不设置技术主键。
     */
    @Test
    void shouldMapConfidentialRegistrationToMainPersistenceObject() {
        ClientRegistration registration = confidentialRegistration();
        registration.disable(UPDATED_AT);

        ClientRegistrationPO result = assembler.toRegistrationPO(registration);

        assertAll(
                () -> assertNull(result.getId()),
                () -> assertEquals("default", result.getTenantId()),
                () -> assertEquals("console-client", result.getClientId()),
                () -> assertEquals("Console Client", result.getDisplayName()),
                () -> assertEquals(SECRET_HASH, result.getClientSecretHash()),
                () -> assertEquals("DISABLED", result.getStatus()),
                () -> assertEquals(900L, result.getAccessTokenTtlSeconds()),
                () -> assertEquals(28800L, result.getRefreshTokenTtlSeconds()),
                () -> assertEquals(300L, result.getAuthorizationCodeTtlSeconds()),
                () -> assertEquals(REGISTERED_AT, result.getRegisteredAt()),
                () -> assertEquals(UPDATED_AT, result.getUpdatedAt())
        );
    }

    /**
     * Given 一个包含多值配置的聚合，When 转换关联表对象，Then 应排序协议值并传递主表技术主键。
     */
    @Test
    void shouldMapAssociatedPersistenceObjectsInStableOrder() {
        ClientRegistration registration = confidentialRegistration();

        List<ClientAuthenticationMethodPO> methods = assembler.toAuthenticationMethodPOs(registration, REGISTRATION_ID);
        List<ClientAuthorizationGrantTypePO> grantTypes = assembler.toGrantTypePOs(registration, REGISTRATION_ID);
        List<ClientRedirectUriPO> redirectUris = assembler.toRedirectUriPOs(registration, REGISTRATION_ID);
        List<ClientScopePO> scopes = assembler.toScopePOs(registration, REGISTRATION_ID);

        assertAll(
                () -> assertEquals(List.of("client_secret_basic", "client_secret_post"),
                        methods.stream().map(ClientAuthenticationMethodPO::getAuthenticationMethod).toList()),
                () -> assertEquals(List.of("authorization_code", "refresh_token"),
                        grantTypes.stream().map(ClientAuthorizationGrantTypePO::getGrantType).toList()),
                () -> assertEquals(List.of("https://client.example/callback", "https://client.example/login"),
                        redirectUris.stream().map(ClientRedirectUriPO::getRedirectUri).toList()),
                () -> assertEquals(List.of("openid", "profile"), scopes.stream().map(ClientScopePO::getScope).toList()),
                () -> assertTrue(methods.stream().allMatch(row -> REGISTRATION_ID.equals(row.getClientRegistrationId()))),
                () -> assertTrue(grantTypes.stream().allMatch(row -> REGISTRATION_ID.equals(row.getClientRegistrationId()))),
                () -> assertTrue(redirectUris.stream().allMatch(row -> REGISTRATION_ID.equals(row.getClientRegistrationId()))),
                () -> assertTrue(scopes.stream().allMatch(row -> REGISTRATION_ID.equals(row.getClientRegistrationId())))
        );
    }

    /**
     * Given 一个完整机密客户端快照，When 恢复领域聚合，Then 应保留配置、密钥和生命周期状态。
     */
    @Test
    void shouldRestoreConfidentialRegistrationFromSnapshot() {
        ClientRegistration source = confidentialRegistration();
        source.disable(UPDATED_AT);
        ClientRegistrationPersistenceSnapshot snapshot = snapshotOf(source);

        ClientRegistration result = assembler.toDomain(snapshot);

        assertAll(
                () -> assertEquals(source.specification(), result.specification()),
                () -> assertEquals(source.secretHash(), result.secretHash()),
                () -> assertEquals(ClientStatus.DISABLED, result.status()),
                () -> assertEquals(REGISTERED_AT, result.registeredAt()),
                () -> assertEquals(UPDATED_AT, result.updatedAt()),
                () -> assertFalse(result.allowsAuthentication())
        );
    }

    /**
     * Given 一个没有密钥摘要的公共客户端快照，When 恢复领域聚合，Then 不应错误创建机密客户端。
     */
    @Test
    void shouldRestorePublicRegistrationWithoutSecret() {
        ClientRegistration source = publicRegistration();

        ClientRegistration result = assembler.toDomain(snapshotOf(source));

        assertAll(
                () -> assertEquals(source.specification(), result.specification()),
                () -> assertTrue(result.secretHash().isEmpty()),
                () -> assertEquals(ClientStatus.ACTIVE, result.status()),
                () -> assertTrue(result.allowsAuthentication())
        );
    }

    /**
     * Given 数据库中的状态或协议值已损坏，When 恢复聚合，Then 应抛出数据读取异常。
     */
    @Test
    void shouldRejectCorruptedPersistedProtocolValues() {
        ClientRegistrationPersistenceSnapshot invalidStatus = snapshotOf(confidentialRegistration());
        invalidStatus.registration().setStatus("UNKNOWN");

        ClientRegistrationPersistenceSnapshot invalidMethod = snapshotOf(confidentialRegistration());
        invalidMethod.authenticationMethods().get(0).setAuthenticationMethod("unsupported_method");

        assertAll(
                () -> assertThrows(DataRetrievalFailureException.class, () -> assembler.toDomain(invalidStatus)),
                () -> assertThrows(DataRetrievalFailureException.class, () -> assembler.toDomain(invalidMethod))
        );
    }

    /**
     * Given 必需参数为空，When 执行任一装配入口，Then 应立即拒绝不完整调用。
     */
    @Test
    void shouldRejectNullAssemblerArguments() {
        ClientRegistration registration = confidentialRegistration();

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> assembler.toRegistrationPO(null)),
                () -> assertThrows(NullPointerException.class, () -> assembler.toAuthenticationMethodPOs(null, REGISTRATION_ID)),
                () -> assertThrows(NullPointerException.class, () -> assembler.toGrantTypePOs(registration, null)),
                () -> assertThrows(NullPointerException.class, () -> assembler.toDomain(null))
        );
    }

    private ClientRegistrationPersistenceSnapshot snapshotOf(ClientRegistration registration) {
        ClientRegistrationPO registrationPO = assembler.toRegistrationPO(registration);
        registrationPO.setId(REGISTRATION_ID);
        return new ClientRegistrationPersistenceSnapshot(
                registrationPO,
                assembler.toAuthenticationMethodPOs(registration, REGISTRATION_ID),
                assembler.toGrantTypePOs(registration, REGISTRATION_ID),
                assembler.toRedirectUriPOs(registration, REGISTRATION_ID),
                assembler.toScopePOs(registration, REGISTRATION_ID)
        );
    }

    private ClientRegistration confidentialRegistration() {
        ClientRegistrationSpec specification = new ClientRegistrationSpec(
                TenantId.DEFAULT_ID,
                new ClientId("console-client"),
                "Console Client",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_POST, ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(AuthorizationGrantType.REFRESH_TOKEN, AuthorizationGrantType.AUTHORIZATION_CODE),
                Set.of(new RedirectUri("https://client.example/login"), new RedirectUri("https://client.example/callback")),
                Set.of("profile", "openid"),
                ClientTokenPolicy.standard()
        );
        return ClientRegistration.registerConfidential(specification, new ClientSecretHash(SECRET_HASH), REGISTERED_AT);
    }

    private ClientRegistration publicRegistration() {
        ClientRegistrationSpec specification = new ClientRegistrationSpec(
                TenantId.DEFAULT_ID,
                new ClientId("browser-client"),
                "Browser Client",
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                Set.of(new RedirectUri("https://browser.example/callback")),
                Set.of("openid"),
                ClientTokenPolicy.standard()
        );
        return ClientRegistration.registerPublic(specification, REGISTERED_AT);
    }
}
