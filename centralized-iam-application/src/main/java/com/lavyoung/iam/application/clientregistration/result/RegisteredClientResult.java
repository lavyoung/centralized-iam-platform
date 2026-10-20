package com.lavyoung.iam.application.clientregistration.result;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * 表示客户端注册用例成功执行后的应用层结果。
 *
 * <p>结果只包含允许跨应用边界返回的客户端信息，
 * 不包含客户端明文密钥或密钥摘要。</p>
 *
 * @param tenantId                    客户端所属租户标识
 * @param clientId                    OAuth2/OIDC 客户端标识
 * @param displayName                 客户端展示名称
 * @param authenticationMethods       客户端认证方式协议值
 * @param grantTypes                  客户端授权类型协议值
 * @param redirectUris                客户端重定向地址
 * @param scopes                      客户端允许申请的权限范围
 * @param accessTokenTimeToLive       访问令牌有效期
 * @param refreshTokenTimeToLive      刷新令牌有效期
 * @param authorizationCodeTimeToLive 授权码有效期
 * @param status                      客户端生命周期状态
 * @param registeredAt                客户端首次注册时间
 * @param updatedAt                   客户端最近更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record RegisteredClientResult(
        String tenantId,
        String clientId,
        String displayName,
        Set<String> authenticationMethods,
        Set<String> grantTypes,
        Set<String> redirectUris,
        Set<String> scopes,
        Duration accessTokenTimeToLive,
        Duration refreshTokenTimeToLive,
        Duration authorizationCodeTimeToLive,
        String status,
        Instant registeredAt,
        Instant updatedAt
) {

    /**
     * 创建集合内容不可变的客户端注册结果。
     *
     * @throws NullPointerException 当任一集合为空或包含空元素时抛出
     */
    public RegisteredClientResult {
        authenticationMethods = Set.copyOf(authenticationMethods);
        grantTypes = Set.copyOf(grantTypes);
        redirectUris = Set.copyOf(redirectUris);
        scopes = Set.copyOf(scopes);
    }
}