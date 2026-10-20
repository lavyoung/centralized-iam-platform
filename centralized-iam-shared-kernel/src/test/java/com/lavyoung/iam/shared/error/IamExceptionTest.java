package com.lavyoung.iam.shared.error;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 IAM 业务异常的错误语义与国际化参数契约。
 *
 * <p>该测试确保异常只携带稳定错误码和不可变参数，
 * 不在共享内核中提前生成某种语言的展示文案。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class IamExceptionTest {

    /**
     * Given 仅提供错误码，When 创建异常，Then 技术消息应为错误码且参数列表为空。
     */
    @Test
    void shouldUseErrorCodeAsTechnicalMessage() {
        // Given
        IamErrorCode errorCode = IamErrorCode.RESOURCE_NOT_FOUND;

        // When
        IamException exception = new IamException(errorCode);

        // Then
        assertAll(
                () -> assertEquals(errorCode, exception.getErrorCode()),
                () -> assertEquals(errorCode.getCode(), exception.getMessage()),
                () -> assertTrue(exception.getMessageArguments().isEmpty())
        );
    }

    /**
     * Given 国际化参数数组，When 创建异常后修改原数组，Then 异常参数保持不可变。
     */
    @Test
    void shouldDefensivelyCopyMessageArguments() {
        // Given
        Object[] arguments = {"tenantId", 1, 64};

        // When
        IamException exception = new IamException(IamErrorCode.TENANT_ID_INVALID, arguments);
        arguments[0] = "changed";

        // Then
        assertAll(
                () -> assertEquals(List.of("tenantId", 1, 64), exception.getMessageArguments()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> exception.getMessageArguments().add("unexpected"))
        );
    }

    /**
     * Given 缺失错误码、参数数组或参数元素，When 创建异常，Then 应立即拒绝。
     */
    @Test
    void shouldRejectMissingErrorComponents() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new IamException(null)),
                () -> assertThrows(NullPointerException.class,
                        () -> new IamException(IamErrorCode.INTERNAL_ERROR, (Object[]) null)),
                () -> assertThrows(NullPointerException.class,
                        () -> new IamException(IamErrorCode.INTERNAL_ERROR, new Object[]{"field", null}))
        );
    }
}
