package com.lavyoung.iam.contract.clientregistration.response;

import java.time.Instant;
import java.util.Set;

/**
 * 表示客户端注册成功后返回的公开配置。
 *
 * <p>响应不包含客户端明文密钥及其摘要。</p>
 *
 * @param tenantId                    客户端所属租户标识
 * @param clientId                    客户端标识
 * @param displayName                 客户端展示名称
 * @param authenticationMethods       客户端认证方式
 * @param grantTypes                  客户端授权类型
 * @param redirectUris                客户端重定向地址
 * @param scopes                      客户端允许申请的权限范围
 * @param accessTokenTtlSeconds       访问令牌有效秒数
 * @param refreshTokenTtlSeconds      刷新令牌有效秒数
 * @param authorizationCodeTtlSeconds 授权码有效秒数
 * @param status                      客户端生命周期状态
 * @param registeredAt                首次注册时间
 * @param updatedAt                   最近更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record ClientRegistrationResponse(
        String tenantId,
        String clientId,
        String displayName,
        Set<String> authenticationMethods,
        Set<String> grantTypes,
        Set<String> redirectUris,
        Set<String> scopes,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds,
        long authorizationCodeTtlSeconds,
        String status,
        Instant registeredAt,
        Instant updatedAt
) {

    /**
     * 对响应集合执行防御性复制。
     *
     * @throws NullPointerException 当任一集合或集合元素为空时抛出
     */
    public ClientRegistrationResponse {
        authenticationMethods = Set.copyOf(authenticationMethods);
        grantTypes = Set.copyOf(grantTypes);
        redirectUris = Set.copyOf(redirectUris);
        scopes = Set.copyOf(scopes);
    }
}