package com.lavyoung.iam.shared.identifier;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证用户标识的生成、解析、值语义和非法输入边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class UserIdTest {

    /**
     * Given 无现有标识，When 连续生成用户标识，Then 应得到不同的 UUID v4。
     */
    @Test
    void shouldGenerateRandomVersionFourIdentifiers() {
        // When
        UserId first = UserId.generate();
        UserId second = UserId.generate();

        // Then
        assertAll(
                () -> assertEquals(4, first.value().version()),
                () -> assertEquals(4, second.value().version()),
                () -> assertNotEquals(first, second)
        );
    }

    /**
     * Given 规范的小写 UUID，When 解析用户标识，Then 应保留对应 UUID 值。
     */
    @Test
    void shouldParseCanonicalLowercaseUuid() {
        String source = "123e4567-e89b-12d3-a456-426614174000";

        UserId userId = UserId.parse(source);

        assertEquals(UUID.fromString(source), userId.value());
    }

    /**
     * Given 相同 UUID，When 创建两个用户标识，Then 应遵循 record 值语义。
     */
    @Test
    void shouldUseValueEquality() {
        UUID value = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        assertEquals(new UserId(value), new UserId(value));
    }

    /**
     * Given 空值或非规范 UUID，When 创建或解析用户标识，Then 应抛出稳定业务错误。
     */
    @Test
    void shouldRejectInvalidUserIdentifiers() {
        assertAll(
                () -> assertInvalid(() -> new UserId(null)),
                () -> assertInvalid(() -> UserId.parse(null)),
                () -> assertInvalid(() -> UserId.parse("")),
                () -> assertInvalid(() -> UserId.parse("123e4567-e89b-12d3-a456-42661417400")),
                () -> assertInvalid(() -> UserId.parse("123E4567-E89B-12D3-A456-426614174000"))
        );
    }

    private void assertInvalid(Runnable action) {
        IamException exception = assertThrows(IamException.class, action::run);

        assertAll(
                () -> assertEquals(IamErrorCode.USER_ID_INVALID, exception.getErrorCode()),
                () -> assertTrue(exception.getMessageArguments().isEmpty())
        );
    }
}
