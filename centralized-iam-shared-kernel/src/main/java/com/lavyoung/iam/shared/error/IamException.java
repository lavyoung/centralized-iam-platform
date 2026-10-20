package com.lavyoung.iam.shared.error;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * 表示 IAM 业务规则或应用用例执行失败。
 *
 * <p>异常携带稳定的 {@link IamErrorCode}，方便不同架构层识别失败语义。
 * 本类型不负责决定 HTTP 状态码或 OAuth2/OIDC 错误响应格式，
 * 这些转换由对应的入站适配器完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Getter
public class IamException extends RuntimeException {

    /**
     * 跨架构层传递的稳定错误语义。
     */
    private final IamErrorCode errorCode;

    /**
     * 国际化资源占位符参数，不得包含敏感信息。
     */
    private final List<Object> messageArguments;

    /**
     * 使用错误码和可选国际化参数创建异常。
     *
     * @param errorCode        IAM 错误码
     * @param messageArguments 国际化资源占位符参数
     * @throws NullPointerException 当错误码、参数数组或任一参数为空时抛出
     */
    public IamException(IamErrorCode errorCode, Object... messageArguments) {
        super(Objects.requireNonNull(errorCode, "errorCode").getCode());
        this.errorCode = errorCode;
        this.messageArguments = List.of(
                Objects.requireNonNull(messageArguments, "messageArguments")
        );
    }

}
