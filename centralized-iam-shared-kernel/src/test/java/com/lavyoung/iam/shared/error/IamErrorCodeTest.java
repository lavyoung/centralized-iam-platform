package com.lavyoung.iam.shared.error;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 IAM 错误码及国际化资源键的稳定契约。
 *
 * <p>该测试防止不同错误语义复用同一编码，并确保共享内核只暴露
 * 可供入站适配器解析的资源键，不直接保存展示文案。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class IamErrorCodeTest {

    /**
     * Given 全部 IAM 错误码，When 提取机器识别码，Then 编码必须唯一且符合统一格式。
     */
    @Test
    void shouldKeepCodesUniqueAndWellFormed() {
        // Given
        IamErrorCode[] errorCodes = IamErrorCode.values();

        // When
        Set<String> uniqueCodes = Arrays.stream(errorCodes)
                .map(IamErrorCode::getCode)
                .collect(Collectors.toUnmodifiableSet());

        // Then
        assertAll(
                () -> assertEquals(errorCodes.length, uniqueCodes.size()),
                () -> assertTrue(uniqueCodes.stream().allMatch(code -> code.matches("IAM-\\d{4}")))
        );
    }

    /**
     * Given 全部 IAM 错误码，When 读取资源键，Then 每个资源键必须属于统一命名空间。
     */
    @Test
    void shouldKeepMessageKeysInIamErrorNamespace() {
        assertTrue(Arrays.stream(IamErrorCode.values())
                .map(IamErrorCode::getMessageKey)
                .allMatch(messageKey -> messageKey.matches("iam\\.error\\.[a-z0-9-]+")));
    }

    /**
     * Given 租户标识错误，When 读取错误契约，Then 应返回稳定编码和资源键。
     */
    @Test
    void shouldExposeConfiguredCodeAndMessageKey() {
        // Given
        IamErrorCode errorCode = IamErrorCode.TENANT_ID_INVALID;

        // When / Then
        assertAll(
                () -> assertEquals("IAM-1003", errorCode.getCode()),
                () -> assertEquals("iam.error.tenant-id-invalid", errorCode.getMessageKey())
        );
    }
}
