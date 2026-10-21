package com.lavyoung.iam.contract.platform.response;

import java.util.Objects;

/**
 * 定义普通 REST 管理接口的统一响应边界。
 *
 * <p>成功和失败使用不同的不可变 record，避免使用可空 {@code data}
 * 或将 {@code Optional} 保存为字段。OAuth2/OIDC 标准端点不使用该响应。</p>
 *
 * @param <T> 成功响应携带的数据类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public sealed interface ResultResponse<T> permits ResultResponse.Success, ResultResponse.Failure {

    /**
     * 普通 REST 请求成功时使用的稳定代码。
     */
    String SUCCESS_CODE = "IAM-0000";

    /**
     * 创建包含非空数据的成功响应。
     *
     * @param data 成功响应数据
     * @param <T>  响应数据类型
     * @return 非空成功响应
     * @throws NullPointerException 当响应数据为空时抛出
     */
    static <T> ResultResponse<T> success(T data) {
        return new Success<>(SUCCESS_CODE, "", Objects.requireNonNull(data, "data"));
    }

    /**
     * 创建不包含成功数据的失败响应。
     *
     * @param code    稳定业务错误码
     * @param message 已完成本地化的安全提示
     * @param <T>     对应成功响应的数据类型
     * @return 非空失败响应
     * @throws NullPointerException 当错误码或提示为空时抛出
     */
    static <T> ResultResponse<T> failure(String code, String message) {
        return new Failure<>(Objects.requireNonNull(code, "code"), Objects.requireNonNull(message, "message"));
    }

    /**
     * 表示带有非空业务数据的成功响应。
     *
     * @param code    稳定成功代码
     * @param message 成功提示，当前为空字符串
     * @param data    非空业务数据
     * @param <T>     响应数据类型
     * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
     * @version 1.0.0
     * @date 2026/10/21
     */
    record Success<T>(String code, String message, T data) implements ResultResponse<T> {
        /**
         * 校验成功响应不包含空字段。
         *
         * @throws NullPointerException 当任一组件为空时抛出
         */
        public Success {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
            Objects.requireNonNull(data, "data");
        }
    }

    /**
     * 表示只包含稳定错误码和本地化提示的失败响应。
     *
     * @param code    稳定业务错误码
     * @param message 已完成本地化的安全提示
     * @param <T>     对应成功响应的数据类型
     * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
     * @version 1.0.0
     * @date 2026/10/21
     */
    record Failure<T>(String code, String message) implements ResultResponse<T> {

        /**
         * 校验失败响应不包含空字段。
         *
         * @throws NullPointerException 当错误码或提示为空时抛出
         */
        public Failure {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }
}
