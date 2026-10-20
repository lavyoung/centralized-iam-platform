package com.lavyoung.iam.contract.clientregistration.request;

import jakarta.validation.constraints.*;

import java.util.Set;

/**
 * 表示注册持有客户端密钥的机密客户端请求。
 *
 * <p>密钥只允许进入当前注册调用链，不得记录、持久化明文或出现在响应中。</p>
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
 * @param clientSecret                客户端明文密钥
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record RegisterConfidentialClientRequest(
        @NotBlank(message = "{iam.validation.client-id.required}")
        String clientId,

        @NotBlank(message = "{iam.validation.display-name.required}")
        String displayName,

        @NotEmpty(message = "{iam.validation.authentication-methods.required}")
        Set<@NotBlank(message = "{iam.validation.collection-element.required}") String> authenticationMethods,

        @NotEmpty(message = "{iam.validation.grant-types.required}")
        Set<@NotBlank(message = "{iam.validation.collection-element.required}") String> grantTypes,

        @NotNull(message = "{iam.validation.redirect-uris.required}")
        Set<@NotBlank(message = "{iam.validation.collection-element.required}") String> redirectUris,

        @NotEmpty(message = "{iam.validation.scopes.required}")
        Set<@NotBlank(message = "{iam.validation.collection-element.required}") String> scopes,

        @Positive(message = "{iam.validation.token-lifetime.positive}")
        long accessTokenTtlSeconds,

        @Positive(message = "{iam.validation.token-lifetime.positive}")
        long refreshTokenTtlSeconds,

        @Positive(message = "{iam.validation.token-lifetime.positive}")
        long authorizationCodeTtlSeconds,

        @NotBlank(message = "{iam.validation.client-secret.required}")
        @Size(min = 32, max = 128, message = "{iam.validation.client-secret.size}")
        String clientSecret
) {

    @Override
    public String toString() {
        return "RegisterConfidentialClientRequest[clientId=%s, displayName=%s, clientSecret=[REDACTED]]"
                .formatted(clientId, displayName);
    }
}