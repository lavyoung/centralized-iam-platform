package com.lavyoung.iam.domain.clientregistration.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 表示 IAM 首期支持的 OAuth2 授权类型。
 *
 * <p>仅建模目标认证中心明确支持的标准授权类型，不包含已经废弃的
 * Resource Owner Password Credentials 授权模式。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Getter
@AllArgsConstructor
public enum AuthorizationGrantType {

    AUTHORIZATION_CODE("authorization_code"),
    CLIENT_CREDENTIALS("client_credentials"),
    REFRESH_TOKEN("refresh_token");

    /**
     * OAuth2 协议中的授权类型值。
     */
    private final String code;

    /**
     * 根据协议值查找授权类型。
     *
     * @param protocolValue OAuth2 协议值
     * @return 匹配的授权类型，不支持时返回空
     */
    public static Optional<AuthorizationGrantType> fromProtocolValue(String protocolValue) {
        return Arrays.stream(values())
                .filter(grantType -> grantType.code.equals(protocolValue))
                .findFirst();
    }
}
