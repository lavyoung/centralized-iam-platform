package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端密钥摘要的非空要求和长度边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientSecretHashTest {

    /**
     * Given 常见算法摘要和长度边界，When 创建值对象，Then 应完整保留编码结果。
     */
    @Test
    void shouldAcceptSupportedSecretHashLengths() {
        assertAll(
                () -> assertAccepted("a".repeat(20)),
                () -> assertAccepted("{bcrypt}$2a$10$abcdefghijklmnopqrstuv123456789012345678901234567890"),
                () -> assertAccepted("a".repeat(512))
        );
    }

    /**
     * Given 空白或越界摘要，When 创建值对象，Then 应抛出稳定领域错误。
     */
    @Test
    void shouldRejectInvalidSecretHash() {
        assertAll(
                () -> assertInvalid(null),
                () -> assertInvalid(""),
                () -> assertInvalid(" ".repeat(20)),
                () -> assertInvalid("a".repeat(19)),
                () -> assertInvalid("a".repeat(513))
        );
    }

    private void assertAccepted(String value) {
        assertEquals(value, new ClientSecretHash(value).value());
    }

    private void assertInvalid(String value) {
        IamException exception = assertThrows(IamException.class, () -> new ClientSecretHash(value));

        assertAll(
                () -> assertEquals(IamErrorCode.CLIENT_SECRET_HASH_INVALID, exception.getErrorCode()),
                () -> assertEquals(List.of(20, 512), exception.getMessageArguments())
        );
    }
}
