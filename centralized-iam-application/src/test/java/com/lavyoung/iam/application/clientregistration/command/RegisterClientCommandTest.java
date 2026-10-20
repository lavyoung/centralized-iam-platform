package com.lavyoung.iam.application.clientregistration.command;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证客户端注册命令的结构校验和集合不可变性。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class RegisterClientCommandTest {

    /**
     * Given 可变的命令集合，When 创建命令后修改原集合，Then 命令内容不应发生变化。
     */
    @Test
    void shouldDefensivelyCopyCollectionValues() {
        // Given
        Set<String> scopes = new HashSet<>(Set.of("openid"));

        // When
        RegisterClientCommand command = validCommand(scopes);
        scopes.add("profile");

        // Then
        assertEquals(Set.of("openid"), command.scopes());
        assertThrows(UnsupportedOperationException.class, () -> command.scopes().add("email"));
    }

    /**
     * Given 缺少必要标量字段，When 创建命令，Then 应返回稳定的注册配置错误。
     */
    @Test
    void shouldRejectMissingScalarValue() {
        // When
        IamException exception = assertThrows(IamException.class, () -> new RegisterClientCommand(
                "default",
                "web-client",
                null,
                Set.of("none"),
                Set.of("authorization_code"),
                Set.of("https://app.example.com/callback"),
                Set.of("openid"),
                Duration.ofMinutes(15),
                Duration.ofHours(8),
                Duration.ofMinutes(5)
        ));

        // Then
        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, exception.getErrorCode());
    }

    /**
     * Given 缺少集合或集合包含空元素，When 创建命令，Then 均应拒绝非法结构。
     */
    @Test
    void shouldRejectInvalidCollectionStructure() {
        // Given
        Set<String> valuesWithNull = new HashSet<>();
        valuesWithNull.add(null);

        // When
        IamException missingCollection = assertThrows(IamException.class, () -> validCommand(null));
        IamException nullElement = assertThrows(IamException.class, () -> validCommand(valuesWithNull));

        // Then
        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, missingCollection.getErrorCode());
        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, nullElement.getErrorCode());
    }

    private RegisterClientCommand validCommand(Set<String> scopes) {
        return new RegisterClientCommand(
                "default",
                "web-client",
                "Web Client",
                Set.of("none"),
                Set.of("authorization_code"),
                Set.of("https://app.example.com/callback"),
                scopes,
                Duration.ofMinutes(15),
                Duration.ofHours(8),
                Duration.ofMinutes(5)
        );
    }
}
