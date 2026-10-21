package com.lavyoung.iam.contract.platform.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证普通 REST 统一响应的成功、失败及非空边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
class ResultResponseTest {

    /**
     * Given 非空业务数据，When 创建成功响应，Then 应返回稳定成功码及原始数据。
     */
    @Test
    void shouldCreateSuccessResponse() {
        // Given
        String data = "registered-client";

        // When
        ResultResponse<String> response = ResultResponse.success(data);

        // Then
        ResultResponse.Success<?> success = assertInstanceOf(ResultResponse.Success.class, response);
        assertAll(
                () -> assertEquals(ResultResponse.SUCCESS_CODE, success.code()),
                () -> assertEquals("", success.message()),
                () -> assertEquals(data, success.data())
        );
    }

    /**
     * Given 稳定错误码和安全提示，When 创建失败响应，Then 不应产生可空数据字段。
     */
    @Test
    void shouldCreateFailureResponseWithoutData() {
        // Given
        String code = "IAM-1000";
        String message = "Invalid request";

        // When
        ResultResponse<String> response = ResultResponse.failure(code, message);

        // Then
        ResultResponse.Failure<?> failure = assertInstanceOf(ResultResponse.Failure.class, response);
        assertAll(
                () -> assertEquals(code, failure.code()),
                () -> assertEquals(message, failure.message())
        );
    }

    /**
     * Given 空响应组件，When 调用响应工厂，Then 应立即拒绝不完整响应。
     */
    @Test
    void shouldRejectNullResponseComponents() {
        // When / Then
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> ResultResponse.success(null)),
                () -> assertThrows(NullPointerException.class, () -> ResultResponse.failure(null, "Invalid request")),
                () -> assertThrows(NullPointerException.class, () -> ResultResponse.failure("IAM-1000", null))
        );
    }
}
