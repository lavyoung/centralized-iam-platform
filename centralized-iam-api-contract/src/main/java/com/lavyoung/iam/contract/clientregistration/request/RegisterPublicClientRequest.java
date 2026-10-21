package com.lavyoung.iam.contract.clientregistration.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

/**
 * 表示注册公共 OAuth2/OIDC 客户端的外部请求。
 *
 * <p>租户标识不由请求方提交，首期由入站适配器注入默认租户，
 * 防止调用方越过租户边界。</p>
 *
 * @param clientId                    客户端标识
 * @param displayName                 客户端展示名称
 * @param authenticationMethods       客户端认证方式
 * @param grantTypes                  客户端授权类型
 * @param redirectUris                客户端重定向地址
 * @param scopes                      客户端允许申请的权限范围
 * @param accessTokenTtlSeconds       访问令牌有效秒数
 * @param refreshTokenTtlSeconds      刷新令牌有效秒数
 * @param authorizationCodeTtlSeconds 授权码有效秒数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record RegisterPublicClientRequest(
        @NotBlank(message = "{validation.client-id.required}")
        String clientId,

        @NotBlank(message = "{validation.display-name.required}")
        String displayName,

        @NotEmpty(message = "{validation.authentication-methods.required}")
        Set<@NotBlank(message = "{validation.collection-element.required}") String> authenticationMethods,

        @NotEmpty(message = "{validation.grant-types.required}")
        Set<@NotBlank(message = "{validation.collection-element.required}") String> grantTypes,

        @NotNull(message = "{validation.redirect-uris.required}")
        Set<@NotBlank(message = "{validation.collection-element.required}") String> redirectUris,

        @NotEmpty(message = "{validation.scopes.required}")
        Set<@NotBlank(message = "{validation.collection-element.required}") String> scopes,

        @Positive(message = "{validation.token-lifetime.positive}")
        long accessTokenTtlSeconds,

        @Positive(message = "{validation.token-lifetime.positive}")
        long refreshTokenTtlSeconds,

        @Positive(message = "{validation.token-lifetime.positive}")
        long authorizationCodeTtlSeconds
) {
}
