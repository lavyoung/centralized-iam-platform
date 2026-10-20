package com.lavyoung.iam.domain.clientregistration.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 表示客户端向令牌端点进行身份认证的方式。
 *
 * <p>协议值遵循 OAuth2/OIDC 标准，但该枚举不依赖 Spring Security。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Getter
@AllArgsConstructor
public enum ClientAuthenticationMethod {

    CLIENT_SECRET_BASIC("client_secret_basic"),
    CLIENT_SECRET_POST("client_secret_post"),
    NONE("none");

    /**
     * OAuth2/OIDC 协议中的认证方式值。
     */
    private final String method;

    /**
     * 根据协议值查找客户端认证方式。
     *
     * @param method OAuth2/OIDC 协议值
     * @return 匹配的认证方式，不支持时返回空
     */
    public static Optional<ClientAuthenticationMethod> fromMethod(String method) {
        return Arrays.stream(values())
                .filter(value -> value.method.equals(method))
                .findFirst();
    }
}
