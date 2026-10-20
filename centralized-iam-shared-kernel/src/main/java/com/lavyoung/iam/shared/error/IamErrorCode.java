package com.lavyoung.iam.shared.error;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 定义 IAM 各架构层之间稳定传递的错误语义。
 *
 * <p>每项错误由机器可识别的稳定编码和国际化资源键组成。
 * 本类型不存放面向用户的展示文案，也不负责绑定 HTTP 状态码。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Getter
@AllArgsConstructor
public enum IamErrorCode {

    INVALID_ARGUMENT("IAM-1000", "iam.error.invalid-argument"),
    RESOURCE_NOT_FOUND("IAM-1001", "iam.error.resource-not-found"),
    RESOURCE_CONFLICT("IAM-1002", "iam.error.resource-conflict"),
    TENANT_ID_INVALID("IAM-1003", "iam.error.tenant-id-invalid"),
    USER_ID_INVALID("IAM-1004", "iam.error.user-id-invalid"),
    CLIENT_ID_INVALID("IAM-1005", "iam.error.client-id-invalid"),

    CLIENT_REDIRECT_URI_INVALID("IAM-1100", "iam.error.client-redirect-uri-invalid"),
    CLIENT_TOKEN_POLICY_INVALID("IAM-1101", "iam.error.client-token-policy-invalid"),
    CLIENT_SECRET_HASH_INVALID("IAM-1102", "iam.error.client-secret-hash-invalid"),
    CLIENT_REGISTRATION_INVALID("IAM-1103", "iam.error.client-registration-invalid"),


    AUTHENTICATION_FAILED("IAM-2000", "iam.error.authentication-failed"),
    ACCESS_DENIED("IAM-2001", "iam.error.access-denied"),

    INTERNAL_ERROR("IAM-9000", "iam.error.internal");

    /**
     * 供客户端和 SDK 稳定识别的错误码。
     */
    private final String code;

    /**
     * 由入站适配器解析的国际化资源键。
     */
    private final String messageKey;

}
