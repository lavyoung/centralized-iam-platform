package com.lavyoung.iam.application.clientregistration.command;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * 表示注册 OAuth2/OIDC 客户端所需的应用层输入。
 *
 * <p>命令仅携带用例输入，不直接复用 API Request 或领域聚合。
 * 应用服务负责将协议值转换为领域值对象并执行领域校验。</p>
 *
 * @param tenantId                    客户端所属租户标识
 * @param clientId                    OAuth2/OIDC 客户端标识
 * @param displayName                 客户端展示名称
 * @param authenticationMethods       客户端认证方式协议值
 * @param grantTypes                  客户端授权类型协议值
 * @param redirectUris                客户端允许使用的重定向地址
 * @param scopes                      客户端允许申请的权限范围
 * @param accessTokenTimeToLive       访问令牌有效期
 * @param refreshTokenTimeToLive      刷新令牌有效期
 * @param authorizationCodeTimeToLive 授权码有效期
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record RegisterClientCommand(
        String tenantId,
        String clientId,
        String displayName,
        Set<String> authenticationMethods,
        Set<String> grantTypes,
        Set<String> redirectUris,
        Set<String> scopes,
        Duration accessTokenTimeToLive,
        Duration refreshTokenTimeToLive,
        Duration authorizationCodeTimeToLive
) {

    /**
     * 创建不可变客户端注册命令。
     *
     * @throws IamException 当必要字段、集合或集合元素为空时抛出
     */
    public RegisterClientCommand {
        if (tenantId == null
                || clientId == null
                || displayName == null
                || accessTokenTimeToLive == null
                || refreshTokenTimeToLive == null
                || authorizationCodeTimeToLive == null) {
            throw invalidCommand();
        }

        authenticationMethods = immutableCopy(authenticationMethods);
        grantTypes = immutableCopy(grantTypes);
        redirectUris = immutableCopy(redirectUris);
        scopes = immutableCopy(scopes);
    }

    private static <T> Set<T> immutableCopy(Set<T> values) {
        if (values == null || values.stream().anyMatch(Objects::isNull)) {
            throw invalidCommand();
        }
        return Set.copyOf(values);
    }

    private static IamException invalidCommand() {
        return new IamException(IamErrorCode.CLIENT_REGISTRATION_INVALID);
    }
}