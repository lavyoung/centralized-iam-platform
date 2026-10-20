package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端注册配置的不可变性、scope 格式和授权类型组合规则。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientRegistrationSpecTest {

    /**
     * Given 可变输入集合和带空白的名称，When 创建配置，Then 应规范名称并生成不可变快照。
     */
    @Test
    void shouldCreateImmutableRegistrationSpecification() {
        Set<String> scopes = new HashSet<>(Set.of("openid", "profile"));
        ClientRegistrationSpec specification = new ClientRegistrationSpec(
                new TenantId("default"),
                new ClientId("web-app"),
                "  Web App  ",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN),
                Set.of(new RedirectUri("https://app.example.com/callback")),
                scopes,
                ClientTokenPolicy.standard()
        );

        scopes.add("admin");

        assertAll(
                () -> assertEquals("Web App", specification.displayName()),
                () -> assertFalse(specification.scopes().contains("admin")),
                () -> assertThrows(UnsupportedOperationException.class, () -> specification.scopes().add("email"))
        );
    }

    /**
     * Given 非法名称或 scope，When 创建配置，Then 应拒绝不稳定的客户端元数据。
     */
    @Test
    void shouldRejectInvalidDisplayNameOrScope() {
        assertAll(
                () -> assertInvalid(() -> createSpecification(" ", Set.of("openid"))),
                () -> assertInvalid(() -> createSpecification("a".repeat(129), Set.of("openid"))),
                () -> assertInvalid(() -> createSpecification("Web App", Set.of())),
                () -> assertInvalid(() -> createSpecification("Web App", Set.of("invalid scope")))
        );
    }

    /**
     * Given 缺少回调地址或基础授权流程的授权类型，When 创建配置，Then 应拒绝非法组合。
     */
    @Test
    void shouldRejectInvalidGrantTypeCombination() {
        assertAll(
                () -> assertInvalid(() -> createSpecification(Set.of(AuthorizationGrantType.AUTHORIZATION_CODE), Set.of())),
                () -> assertInvalid(() -> createSpecification(Set.of(AuthorizationGrantType.REFRESH_TOKEN), Set.of()))
        );
    }

    private ClientRegistrationSpec createSpecification(String displayName, Set<String> scopes) {
        return new ClientRegistrationSpec(
                new TenantId("default"),
                new ClientId("web-app"),
                displayName,
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                Set.of(),
                scopes,
                ClientTokenPolicy.standard()
        );
    }

    private ClientRegistrationSpec createSpecification(Set<AuthorizationGrantType> grantTypes, Set<RedirectUri> redirectUris) {
        return new ClientRegistrationSpec(
                new TenantId("default"),
                new ClientId("web-app"),
                "Web App",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                grantTypes,
                redirectUris,
                Set.of("openid"),
                ClientTokenPolicy.standard()
        );
    }

    private void assertInvalid(Executable creation) {
        IamException exception = assertThrows(IamException.class, creation);

        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, exception.getErrorCode());
    }
}
