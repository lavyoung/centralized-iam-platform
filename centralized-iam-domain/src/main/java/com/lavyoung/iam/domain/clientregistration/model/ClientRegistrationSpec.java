package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 表示客户端注册聚合所需的不可变协议配置。
 *
 * @param tenantId              客户端所属租户
 * @param clientId              OAuth2/OIDC 客户端标识
 * @param displayName           客户端展示名称
 * @param authenticationMethods 客户端认证方式
 * @param grantTypes            客户端允许使用的授权类型
 * @param redirectUris          已注册的精确重定向地址
 * @param scopes                客户端允许申请的 scope
 * @param tokenPolicy           客户端令牌生命周期策略
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record ClientRegistrationSpec(
        TenantId tenantId,
        ClientId clientId,
        String displayName,
        Set<ClientAuthenticationMethod> authenticationMethods,
        Set<AuthorizationGrantType> grantTypes,
        Set<RedirectUri> redirectUris,
        Set<String> scopes,
        ClientTokenPolicy tokenPolicy
) {

    private static final int MAX_DISPLAY_NAME_LENGTH = 128;
    private static final Pattern SCOPE_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    /**
     * 校验并创建客户端注册配置。
     *
     * @param tenantId              客户端所属租户
     * @param clientId              OAuth2/OIDC 客户端标识
     * @param displayName           客户端展示名称
     * @param authenticationMethods 客户端认证方式
     * @param grantTypes            客户端允许使用的授权类型
     * @param redirectUris          已注册的精确重定向地址
     * @param scopes                客户端允许申请的 scope
     * @param tokenPolicy           客户端令牌生命周期策略
     * @throws IamException 当必填配置缺失或授权类型组合违反协议约束时抛出
     */
    public ClientRegistrationSpec {
        if (tenantId == null || clientId == null || tokenPolicy == null) {
            throw invalidRegistration();
        }

        displayName = normalizeDisplayName(displayName);
        authenticationMethods = copyRequired(authenticationMethods);
        grantTypes = copyRequired(grantTypes);
        redirectUris = copyOptional(redirectUris);
        scopes = copyRequired(scopes);

        validateScopes(scopes);
        validateGrantTypes(grantTypes, redirectUris);
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw invalidRegistration();
        }

        String normalized = displayName.strip();
        if (normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw invalidRegistration();
        }
        return normalized;
    }

    private static <T> Set<T> copyRequired(Set<T> values) {
        if (values == null || values.isEmpty() || containsNull(values)) {
            throw invalidRegistration();
        }
        return Set.copyOf(values);
    }

    private static <T> Set<T> copyOptional(Set<T> values) {
        if (values == null || containsNull(values)) {
            throw invalidRegistration();
        }
        return Set.copyOf(values);
    }

    private static boolean containsNull(Set<?> values) {
        return values.stream().anyMatch(Objects::isNull);
    }

    private static void validateScopes(Set<String> scopes) {
        if (scopes.stream().anyMatch(scope -> scope == null || !SCOPE_PATTERN.matcher(scope).matches())) {
            throw invalidRegistration();
        }
    }

    private static void validateGrantTypes(Set<AuthorizationGrantType> grantTypes, Set<RedirectUri> redirectUris) {
        boolean authorizationCodeEnabled = grantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE);
        boolean refreshTokenEnabled = grantTypes.contains(AuthorizationGrantType.REFRESH_TOKEN);

        if (authorizationCodeEnabled && redirectUris.isEmpty()) {
            throw invalidRegistration();
        }
        if (refreshTokenEnabled && !authorizationCodeEnabled) {
            throw invalidRegistration();
        }
    }

    private static IamException invalidRegistration() {
        return new IamException(IamErrorCode.CLIENT_REGISTRATION_INVALID);
    }
}
